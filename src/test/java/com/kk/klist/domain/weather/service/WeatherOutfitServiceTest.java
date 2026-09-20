package com.kk.klist.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.kk.klist.domain.weather.dto.response.WeatherOutfitResponse;
import com.kk.klist.global.exception.WeatherErrorCode;
import com.kk.klist.global.exception.WeatherException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WeatherOutfitServiceTest {

    private static final DateTimeFormatter FCST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.ResponseSpec responseSpec;
    private WeatherOutfitService weatherOutfitService;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);

        weatherOutfitService = new WeatherOutfitService(restClient);
        ReflectionTestUtils.setField(weatherOutfitService, "serviceKey", "test-auth-key");
    }

    @ParameterizedTest(name = "latitude={0}, longitude={1}")
    @CsvSource({
            "91, 127",
            "-91, 127",
            "37.5, 181",
            "37.5, -181",
            "NaN, 127",
            "37.5, NaN"
    })
    void 좌표가_유효범위를_벗어나면_예외가_발생하고_외부_API를_호출하지_않는다(double latitude, double longitude) {
        assertThatThrownBy(() -> weatherOutfitService.getWeatherOutfit(latitude, longitude, "ko"))
                .isInstanceOf(WeatherException.class)
                .extracting(e -> ((WeatherException) e).getErrorCode())
                .isEqualTo(WeatherErrorCode.INVALID_COORDINATES);

        assertThat(mockingDetails(restClient).getInvocations()).isEmpty();
    }

    @Test
    void 현재_날씨와_옷차림을_정상적으로_조회한다() {
        LocalDateTime now = LocalDateTime.now();
        JsonNode ncstResponse = ncstJson(26.5, 0.0, 0, 1.5);
        JsonNode fcstResponse = fcstJson(
                new ForecastFixture(now.plusHours(2), 36.0, 0),
                new ForecastFixture(now.plusHours(3), 30.0, 1)
        );

        when(responseSpec.body(JsonNode.class)).thenReturn(ncstResponse, fcstResponse);

        WeatherOutfitResponse response = weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko");

        assertThat(response.weather().temperature()).isEqualTo(26.5);
        assertThat(response.weather().precipitation()).isEqualTo(0.0);
        assertThat(response.weather().condition()).isEqualTo("SUNNY");

        assertThat(response.outfit().temperatureRange()).isEqualTo("23~27");
        assertThat(response.outfit().items()).containsExactly("반팔", "얇은 셔츠", "반바지", "면바지");
        assertThat(response.outfit().additionalTips())
                .anyMatch(tip -> tip.contains("일교차가 크니 겉옷을 챙기세요"));
        assertThat(response.outfit().additionalTips())
                .anyMatch(tip -> tip.contains("비/눈이 예상되니 우산을 준비하세요"));
    }

    @Test
    void 일교차가_8도_미만이고_강수가_없으면_추가_팁이_붙지_않는다() {
        LocalDateTime now = LocalDateTime.now();
        JsonNode ncstResponse = ncstJson(20.0, 0.0, 0, 1.0);
        JsonNode fcstResponse = fcstJson(
                new ForecastFixture(now.plusHours(2), 24.0, 0),
                new ForecastFixture(now.plusHours(4), 22.0, 0)
        );

        when(responseSpec.body(JsonNode.class)).thenReturn(ncstResponse, fcstResponse);

        WeatherOutfitResponse response = weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko");

        assertThat(response.outfit().additionalTips())
                .noneMatch(tip -> tip.contains("일교차") || tip.contains("우산"));
    }

    @Test
    void lang이_en이면_영어로_응답한다() {
        when(responseSpec.body(JsonNode.class)).thenReturn(ncstJson(15.0, 0.0, 0, 10.0), fcstJson());

        WeatherOutfitResponse response = weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "en");

        assertThat(response.outfit().items()).containsExactly("Jacket", "Cardigan", "Field jacket", "Tights", "Jeans");
        assertThat(response.outfit().additionalTips()).containsExactly("Windbreaker recommended, hold onto your hat");
    }

    @Test
    void 강풍이면_강풍_조건_팁이_붙는다() {
        JsonNode ncstResponse = ncstJson(15.0, 0.0, 0, 10.0);
        JsonNode fcstResponse = fcstJson();

        when(responseSpec.body(JsonNode.class)).thenReturn(ncstResponse, fcstResponse);

        WeatherOutfitResponse response = weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko");

        assertThat(response.weather().condition()).isEqualTo("WINDY");
        assertThat(response.outfit().additionalTips()).containsExactly("바람막이, 모자류 주의");
    }

    @Test
    void 현재_날씨_API_호출이_실패하면_예외가_발생한다() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new RestClientException("연결 실패"));

        assertThatThrownBy(() -> weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko"))
                .isInstanceOf(WeatherException.class)
                .extracting(e -> ((WeatherException) e).getErrorCode())
                .isEqualTo(WeatherErrorCode.WEATHER_API_ERROR);
    }

    @Test
    void 예보_조회만_실패하면_현재_날씨_기준으로는_정상_응답한다() {
        JsonNode ncstResponse = ncstJson(10.0, 0.0, 1, 2.0);

        when(responseSpec.body(JsonNode.class))
                .thenReturn(ncstResponse)
                .thenThrow(new RestClientException("예보 연결 실패"));

        WeatherOutfitResponse response = weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko");

        assertThat(response.weather().temperature()).isEqualTo(10.0);
        assertThat(response.weather().condition()).isEqualTo("RAINY");
        assertThat(response.outfit().temperatureRange()).isEqualTo("9~11");
        assertThat(response.outfit().additionalTips()).containsExactly("우산, 방수 아우터, 장화 권장");
    }

    @Test
    void 기상청_응답코드가_실패면_예외가_발생한다() {
        JsonNode failedResponse = JSON_MAPPER.readTree("""
                {
                  "response": {
                    "header": { "resultCode": "03", "resultMsg": "NODATA_ERROR" }
                  }
                }
                """);
        when(responseSpec.body(JsonNode.class)).thenReturn(failedResponse);

        assertThatThrownBy(() -> weatherOutfitService.getWeatherOutfit(37.5665, 126.9780, "ko"))
                .isInstanceOf(WeatherException.class)
                .extracting(e -> ((WeatherException) e).getErrorCode())
                .isEqualTo(WeatherErrorCode.WEATHER_API_ERROR);
    }

    private JsonNode ncstJson(double t1h, double rn1, int pty, double wsd) {
        String json = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "OK" },
                    "body": {
                      "items": {
                        "item": [
                          { "category": "T1H", "obsrValue": "%s" },
                          { "category": "RN1", "obsrValue": "%s" },
                          { "category": "PTY", "obsrValue": "%d" },
                          { "category": "WSD", "obsrValue": "%s" }
                        ]
                      }
                    }
                  }
                }
                """.formatted(t1h, rn1, pty, wsd);
        return JSON_MAPPER.readTree(json);
    }

    private JsonNode fcstJson(ForecastFixture... fixtures) {
        StringBuilder items = new StringBuilder();
        for (ForecastFixture fixture : fixtures) {
            String dateTime = fixture.dateTime().format(FCST_TIME_FORMATTER);
            String date = dateTime.substring(0, 8);
            String time = dateTime.substring(8);
            if (!items.isEmpty()) {
                items.append(",");
            }
            items.append("""
                    { "category": "T1H", "fcstDate": "%s", "fcstTime": "%s", "fcstValue": "%s" },
                    { "category": "PTY", "fcstDate": "%s", "fcstTime": "%s", "fcstValue": "%d" }
                    """.formatted(date, time, fixture.temperature(), date, time, fixture.pty()));
        }

        String json = """
                {
                  "response": {
                    "header": { "resultCode": "00", "resultMsg": "OK" },
                    "body": {
                      "items": {
                        "item": [%s]
                      }
                    }
                  }
                }
                """.formatted(items);
        return JSON_MAPPER.readTree(json);
    }

    private record ForecastFixture(LocalDateTime dateTime, double temperature, int pty) {
    }
}
