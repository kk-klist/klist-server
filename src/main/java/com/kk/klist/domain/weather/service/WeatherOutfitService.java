package com.kk.klist.domain.weather.service;

import tools.jackson.databind.JsonNode;
import com.kk.klist.domain.weather.dto.response.WeatherOutfitResponse;
import com.kk.klist.domain.weather.dto.response.WeatherOutfitResponse.OutfitInfo;
import com.kk.klist.domain.weather.dto.response.WeatherOutfitResponse.WeatherInfo;
import com.kk.klist.domain.weather.vo.WeatherCondition;
import com.kk.klist.global.exception.WeatherErrorCode;
import com.kk.klist.global.exception.WeatherException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherOutfitService {

    private static final double TEMPERATURE_DELTA_THRESHOLD = 8.0;
    private static final int FORECAST_LOOKAHEAD_HOURS = 6;
    private static final int SUMMER_MONTH_START = 6;
    private static final int SUMMER_MONTH_END = 8;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH00");
    private static final DateTimeFormatter HALF_HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH30");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final RestClient kmaRestClient;

    @Value("${weather.api.key}")
    private String serviceKey;

    public WeatherOutfitResponse getWeatherOutfit(double latitude, double longitude, String lang) {
        validateCoordinates(latitude, longitude);

        int[] grid = toGrid(latitude, longitude);
        LocalDateTime now = LocalDateTime.now();

        CurrentWeather current = fetchCurrentWeather(grid[0], grid[1], now);
        List<ForecastSlot> forecast = fetchForecastSafely(grid[0], grid[1], now);

        WeatherInfo weatherInfo = new WeatherInfo(
                current.temperature(),
                current.precipitation(),
                current.condition().name(),
                current.observedAt()
        );

        return new WeatherOutfitResponse(weatherInfo, buildOutfit(current, forecast, "en".equalsIgnoreCase(lang)));
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }
    }

    private CurrentWeather fetchCurrentWeather(int nx, int ny, LocalDateTime now) {
        LocalDateTime baseDateTime = now.getMinute() < 40 ? now.minusHours(1) : now;
        JsonNode items = callKmaApi("/getUltraSrtNcst", nx, ny,
                baseDateTime.format(DATE_FORMATTER), baseDateTime.format(HOUR_FORMATTER));

        double temperature = 0;
        double precipitation = 0;
        int pty = 0;
        double windSpeed = 0;

        for (JsonNode item : items) {
            String category = item.path("category").asString();
            double value = parseDouble(item.path("obsrValue").asString());
            switch (category) {
                case "T1H" -> temperature = value;
                case "RN1" -> precipitation = value;
                case "PTY" -> pty = (int) value;
                case "WSD" -> windSpeed = value;
                default -> {
                }
            }
        }

        WeatherCondition condition = WeatherCondition.fromPty(pty, windSpeed);
        LocalDateTime observedAt = baseDateTime.withMinute(0).withSecond(0).withNano(0);
        return new CurrentWeather(temperature, precipitation, condition, observedAt);
    }

    private List<ForecastSlot> fetchForecastSafely(int nx, int ny, LocalDateTime now) {
        try {
            return fetchForecast(nx, ny, now);
        } catch (Exception e) {
            log.warn("[WeatherForecast] 예보 조회 실패로 미래 팁 생성을 건너뜁니다. message={}", e.getMessage());
            return List.of();
        }
    }

    private List<ForecastSlot> fetchForecast(int nx, int ny, LocalDateTime now) {
        LocalDateTime baseDateTime = resolveForecastBaseTime(now);
        JsonNode items = callKmaApi("/getUltraSrtFcst", nx, ny,
                baseDateTime.format(DATE_FORMATTER), baseDateTime.format(HALF_HOUR_FORMATTER));

        Map<LocalDateTime, Double> temperatures = new TreeMap<>();
        Map<LocalDateTime, Integer> ptyByTime = new TreeMap<>();
        LocalDateTime horizon = now.plusHours(FORECAST_LOOKAHEAD_HOURS);

        for (JsonNode item : items) {
            LocalDateTime fcstDateTime = LocalDateTime.parse(
                    item.path("fcstDate").asString() + item.path("fcstTime").asString(), DATE_TIME_FORMATTER);

            if (fcstDateTime.isBefore(now) || fcstDateTime.isAfter(horizon)) {
                continue;
            }

            String category = item.path("category").asString();
            double value = parseDouble(item.path("fcstValue").asString());
            if ("T1H".equals(category)) {
                temperatures.put(fcstDateTime, value);
            } else if ("PTY".equals(category)) {
                ptyByTime.put(fcstDateTime, (int) value);
            }
        }

        List<ForecastSlot> slots = new ArrayList<>();
        temperatures.forEach((time, temp) -> slots.add(new ForecastSlot(time, temp, ptyByTime.getOrDefault(time, 0))));
        return slots;
    }

    private LocalDateTime resolveForecastBaseTime(LocalDateTime now) {
        LocalDateTime candidate = now.getMinute() < 45 ? now.minusHours(1) : now;
        return candidate.withMinute(30).withSecond(0).withNano(0);
    }

    private JsonNode callKmaApi(String path, int nx, int ny, String baseDate, String baseTime) {
        JsonNode root;
        try {
            root = kmaRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("authKey", serviceKey)
                            .queryParam("dataType", "JSON")
                            .queryParam("numOfRows", 100)
                            .queryParam("pageNo", 1)
                            .queryParam("base_date", baseDate)
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", nx)
                            .queryParam("ny", ny)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            log.info("[WeatherApi] 호출 성공. path={}, base_date={}, base_time={}, nx={}, ny={}", path, baseDate, baseTime, nx, ny);
            log.debug("[WeatherApi] 응답 원문. path={}, body={}", path, root);
        } catch (Exception e) {
            log.warn("[WeatherApi] 호출 실패. path={}, message={}", path, e.getMessage());
            throw new WeatherException(WeatherErrorCode.WEATHER_API_ERROR);
        }

        if (root == null || !"00".equals(root.path("response").path("header").path("resultCode").asString())) {
            throw new WeatherException(WeatherErrorCode.WEATHER_API_ERROR);
        }

        return root.path("response").path("body").path("items").path("item");
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private OutfitInfo buildOutfit(CurrentWeather current, List<ForecastSlot> forecast, boolean en) {
        TemperatureBand band = resolveTemperatureBand(current.temperature(), en);
        List<String> tips = new ArrayList<>(resolveConditionTips(current.condition(), en));

        forecast.stream()
                .mapToDouble(ForecastSlot::temperature)
                .max()
                .ifPresent(maxTemp -> {
                    if (maxTemp - current.temperature() >= TEMPERATURE_DELTA_THRESHOLD) {
                        tips.add(en ? "Big temperature swing, so bring a jacket." : "일교차가 크니 겉옷을 챙기세요.");
                    }
                });

        forecast.stream()
                .filter(slot -> slot.pty() != 0)
                .findFirst()
                .ifPresent(slot -> {
                    long hoursUntil = Duration.between(LocalDateTime.now(), slot.dateTime()).toHours();
                    tips.add(en
                            ? "Rain or snow expected " + (hoursUntil <= 0 ? "soon" : "in " + hoursUntil + "h") + ", so bring an umbrella."
                            : (hoursUntil <= 0 ? "곧" : hoursUntil + "시간 후") + " 비/눈이 예상되니 우산을 준비하세요.");
                });

        return new OutfitInfo(band.label(), band.items(), tips);
    }

    private TemperatureBand resolveTemperatureBand(double temperature, boolean en) {
        if (temperature >= 28) {
            return new TemperatureBand("28~", en ? List.of("Tank top", "T-shirt", "Shorts", "Dress") : List.of("민소매", "반팔", "반바지", "원피스"));
        }
        if (temperature >= 23) {
            return new TemperatureBand("23~27", en ? List.of("T-shirt", "Light shirt", "Shorts", "Cotton pants") : List.of("반팔", "얇은 셔츠", "반바지", "면바지"));
        }
        if (temperature >= 20) {
            return new TemperatureBand("20~22", en ? List.of("Light cardigan", "Long sleeve", "Cotton pants", "Jeans") : List.of("얇은 가디건", "긴팔", "면바지", "청바지"));
        }
        if (temperature >= 17) {
            return new TemperatureBand("17~19", en ? List.of("Light knit", "Sweatshirt", "Cardigan", "Jeans") : List.of("얇은 니트", "맨투맨", "가디건", "청바지"));
        }
        if (temperature >= 12) {
            return new TemperatureBand("12~16", en ? List.of("Jacket", "Cardigan", "Field jacket", "Tights", "Jeans") : List.of("자켓", "가디건", "야상", "스타킹", "청바지"));
        }
        if (temperature >= 9) {
            return new TemperatureBand("9~11", en ? List.of("Trench coat", "Field jacket", "Jacket", "Knit", "Jeans") : List.of("트렌치코트", "야상", "자켓", "니트", "청바지"));
        }
        if (temperature >= 5) {
            return new TemperatureBand("5~8", en ? List.of("Coat", "Leather jacket", "Thermal wear", "Knit", "Leggings") : List.of("코트", "가죽자켓", "히트텍", "니트", "레깅스"));
        }
        return new TemperatureBand("~4", en ? List.of("Padded jacket", "Heavy coat", "Scarf", "Fleece-lined items") : List.of("패딩", "두꺼운 코트", "목도리", "기모 제품"));
    }

    private List<String> resolveConditionTips(WeatherCondition condition, boolean en) {
        List<String> tips = new ArrayList<>();
        switch (condition) {
            case RAINY, SLEET, SHOWER -> tips.add(en ? "Umbrella, waterproof outerwear, rain boots recommended" : "우산, 방수 아우터, 장화 권장");
            case SNOWY -> tips.add(en ? "Warm waterproof shoes, watch for slippery roads" : "방한·방수 신발, 미끄럼 주의");
            case WINDY -> tips.add(en ? "Windbreaker recommended, hold onto your hat" : "바람막이, 모자류 주의");
            case SUNNY -> {
                int month = LocalDate.now().getMonthValue();
                if (month >= SUMMER_MONTH_START && month <= SUMMER_MONTH_END) {
                    tips.add(en ? "Hat, sunglasses, sunscreen recommended" : "모자, 선글라스, 선크림 권장");
                }
            }
        }
        return tips;
    }

    /**
     * 기상청 격자(nx, ny) 변환 공식 (Lambert Conformal Conic 투영).
     */
    private int[] toGrid(double lat, double lon) {
        double re = 6371.00877 / 5.0;
        double degrad = Math.PI / 180.0;
        double slat1 = 30.0 * degrad;
        double slat2 = 60.0 * degrad;
        double olon = 126.0 * degrad;
        double olat = 38.0 * degrad;
        double xo = 43;
        double yo = 136;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * degrad * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * degrad - olon;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + xo + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + yo + 0.5);

        return new int[] {nx, ny};
    }

    private record CurrentWeather(
            double temperature,
            double precipitation,
            WeatherCondition condition,
            LocalDateTime observedAt
    ) {
    }

    private record ForecastSlot(
            LocalDateTime dateTime,
            double temperature,
            int pty
    ) {
    }

    private record TemperatureBand(
            String label,
            List<String> items
    ) {
    }
}
