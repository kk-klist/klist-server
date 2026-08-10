package com.kk.klist.domain.tour.service;

import com.kk.klist.domain.tour.domain.exception.TourErrorCode;
import com.kk.klist.domain.tour.domain.exception.TourException;
import com.kk.klist.domain.tour.dto.response.TourDetailResponse;
import com.kk.klist.domain.tour.dto.response.TourSpotResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourService {

    private static final int DEFAULT_NEARBY_ROWS = 50;
    private static final int DEFAULT_SEARCH_ROWS = 30;
    private static final int DEFAULT_FESTIVAL_ROWS = 50;
    private static final DateTimeFormatter EVENT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient tourRestClient;

    @Value("${tour.api.base-url}")
    private String baseUrl;

    @Value("${tour.api.service-key}")
    private String serviceKey;

    @Value("${tour.api.mobile-app}")
    private String mobileApp;

    @Cacheable(value = "tourNearby", key = "#lat + ',' + #lng + ',' + #radius + ',' + #category + ',' + #lang")
    public List<TourSpotResponse> findNearby(double lat, double lng, int radius, String category, String lang) {
        Map<String, String> params = commonParams();
        params.put("mapX", String.valueOf(lng));
        params.put("mapY", String.valueOf(lat));
        params.put("radius", String.valueOf(radius));
        params.put("arrange", "E");
        params.put("numOfRows", String.valueOf(DEFAULT_NEARBY_ROWS));
        if (category != null && !category.isBlank()) {
            params.put("contentTypeId", category);
        }

        JsonNode root = callTourApi("/" + service(lang) + "/locationBasedList2", params);
        List<TourSpotResponse> spots = parseSpots(root);
        log.info("[Tour] 근접 조회 완료. lat={}, lng={}, radius={}, count={}", lat, lng, radius, spots.size());
        return spots;
    }

    @Cacheable(value = "tourSearch", key = "#keyword + ',' + #lang")
    public List<TourSpotResponse> search(String keyword, String lang) {
        Map<String, String> params = commonParams();
        params.put("keyword", keyword);
        params.put("arrange", "A");
        params.put("numOfRows", String.valueOf(DEFAULT_SEARCH_ROWS));

        JsonNode root = callTourApi("/" + service(lang) + "/searchKeyword2", params);
        List<TourSpotResponse> spots = parseSpots(root);
        log.info("[Tour] 키워드 검색 완료. keyword={}, count={}", keyword, spots.size());
        return spots;
    }

    @Cacheable(value = "tourFestival", key = "#eventStartDate + ',' + #areaCode + ',' + #lang")
    public List<TourSpotResponse> findFestivals(String eventStartDate, String areaCode, String lang) {
        String date = (eventStartDate == null || eventStartDate.isBlank())
                ? LocalDate.now().format(EVENT_DATE_FORMATTER)
                : eventStartDate;

        Map<String, String> params = commonParams();
        params.put("eventStartDate", date);
        params.put("arrange", "A");
        params.put("numOfRows", String.valueOf(DEFAULT_FESTIVAL_ROWS));
        if (areaCode != null && !areaCode.isBlank()) {
            params.put("areaCode", areaCode);
        }

        JsonNode root = callTourApi("/" + service(lang) + "/searchFestival2", params);
        List<TourSpotResponse> spots = parseSpots(root);
        log.info("[Tour] 축제 조회 완료. eventStartDate={}, areaCode={}, count={}", date, areaCode, spots.size());
        return spots;
    }

    @Cacheable(value = "tourDetail", key = "#contentId + ',' + #contentTypeId + ',' + #lang")
    public TourDetailResponse findDetail(String contentId, String contentTypeId, String lang) {
        Map<String, String> commonRequestParams = commonParams();
        commonRequestParams.put("contentId", contentId);

        JsonNode commonItem = firstItem(callTourApi("/" + service(lang) + "/detailCommon2", commonRequestParams));
        if (commonItem == null) {
            throw new TourException(TourErrorCode.TOUR_DETAIL_NOT_FOUND);
        }

        Map<String, String> imageRequestParams = new LinkedHashMap<>(commonRequestParams);
        imageRequestParams.put("imageYN", "Y");
        List<String> images = new ArrayList<>();
        for (JsonNode node : itemArray(callTourApi("/" + service(lang) + "/detailImage2", imageRequestParams))) {
            String url = node.path("originimgurl").asString();
            if (url != null && !url.isBlank()) {
                images.add(url);
            }
        }

        String useTime = null;
        if (contentTypeId != null && !contentTypeId.isBlank()) {
            Map<String, String> introRequestParams = commonParams();
            introRequestParams.put("contentId", contentId);
            introRequestParams.put("contentTypeId", contentTypeId);
            JsonNode introItem = firstItem(callTourApi("/" + service(lang) + "/detailIntro2", introRequestParams));
            useTime = firstNonBlank(introItem, "usetime", "opentime", "usetimeculture", "playtime", "opentimefood");
        }

        String resolvedContentTypeId = commonItem.path("contenttypeid").asString();
        if (resolvedContentTypeId == null || resolvedContentTypeId.isBlank()) {
            resolvedContentTypeId = contentTypeId;
        }

        TourDetailResponse response = new TourDetailResponse(
                commonItem.path("contentid").asString(),
                resolvedContentTypeId,
                commonItem.path("title").asString(),
                parseDouble(commonItem.path("mapy")),
                parseDouble(commonItem.path("mapx")),
                emptyToNull(commonItem.path("firstimage").asString()),
                commonItem.path("addr1").asString(),
                emptyToNull(commonItem.path("overview").asString()),
                useTime,
                images
        );
        log.info("[Tour] 상세 조회 완료. contentId={}, images={}", contentId, images.size());
        return response;
    }

    /**
     * TourAPI 서비스는 언어별로 별도 엔드포인트를 제공한다 (KorService2/EngService2/...).
     */
    private String service(String lang) {
        if (lang == null) {
            return "KorService2";
        }
        return switch (lang) {
            case "en" -> "EngService2";
            case "ja" -> "JpnService2";
            case "zh-CN" -> "ChsService2";
            case "zh-TW" -> "ChtService2";
            default -> "KorService2";
        };
    }

    private Map<String, String> commonParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MobileOS", "ETC");
        params.put("MobileApp", mobileApp);
        params.put("_type", "json");
        return params;
    }

    private JsonNode callTourApi(String path, Map<String, String> params) {
        // serviceKey 는 공공데이터포털 Encoding 키를 그대로 써야 하며 uriBuilder 로 조립하면 이중 인코딩된다.
        // RestClient 에 상대 URI 를 넘기면 baseUrl 의 서비스 세그먼트(/B551011)가 잘려나가므로
        // baseUrl 을 포함한 절대 URI 로 직접 조립한다.
        StringBuilder query = new StringBuilder("serviceKey=").append(serviceKey);
        params.forEach((key, value) ->
                query.append('&').append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)));
        URI uri = URI.create(baseUrl + path + "?" + query);

        JsonNode root;
        try {
            root = tourRestClient.get().uri(uri).retrieve().body(JsonNode.class);
            log.info("[TourApi] 호출 성공. path={}", path);
        } catch (Exception e) {
            log.warn("[TourApi] 호출 실패. path={}, message={}", path, e.getMessage());
            throw new TourException(TourErrorCode.TOUR_API_ERROR);
        }

        if (root == null) {
            throw new TourException(TourErrorCode.TOUR_API_ERROR);
        }
        return root;
    }

    private List<TourSpotResponse> parseSpots(JsonNode root) {
        List<TourSpotResponse> spots = new ArrayList<>();
        for (JsonNode node : itemArray(root)) {
            spots.add(toSpot(node));
        }
        return spots;
    }

    private TourSpotResponse toSpot(JsonNode node) {
        return new TourSpotResponse(
                node.path("contentid").asString(),
                node.path("contenttypeid").asString(),
                node.path("title").asString(),
                parseDouble(node.path("mapy")),
                parseDouble(node.path("mapx")),
                emptyToNull(node.path("firstimage").asString()),
                node.path("addr1").asString(),
                parseDouble(node.path("dist"))
        );
    }

    /**
     * TourAPI 응답 items 는 결과 없을 때 빈 문자열 "" 로 오기도 한다 → 방어적으로 조회.
     */
    private List<JsonNode> itemArray(JsonNode root) {
        if (root == null) {
            return List.of();
        }
        JsonNode items = root.path("response").path("body").path("items");
        if (items == null || !items.isObject()) {
            return List.of();
        }
        JsonNode item = items.path("item");
        List<JsonNode> list = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(list::add);
        } else if (item.isObject()) {
            list.add(item);
        }
        return list;
    }

    private JsonNode firstItem(JsonNode root) {
        List<JsonNode> array = itemArray(root);
        return array.isEmpty() ? null : array.get(0);
    }

    private Double parseDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String raw = node.asString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String firstNonBlank(JsonNode item, String... fields) {
        if (item == null) {
            return null;
        }
        for (String field : fields) {
            String value = item.path(field).asString();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
