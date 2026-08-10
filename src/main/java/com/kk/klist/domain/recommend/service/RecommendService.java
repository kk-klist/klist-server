package com.kk.klist.domain.recommend.service;

import com.kk.klist.domain.recommend.domain.exception.RecommendErrorCode;
import com.kk.klist.domain.recommend.domain.exception.RecommendException;
import com.kk.klist.domain.recommend.dto.response.RecommendPlaceResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
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

/**
 * K-컬처 장르별 추천 — 공모전 필수 데이터인 한국관광공사 TourAPI(searchKeyword2)로 큐레이션한다.
 * 각 장르는 K-컬처 대표 명소 키워드 목록을 갖고, 키워드별 검색 결과를 합쳐 dedup+거리 정렬 후 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    // 장르별 TourAPI 검색 키워드 (큐레이션). 한 장르에 여러 키워드로 검색해 결과 합침.
    private static final Map<String, List<String>> KEYWORDS_BY_GENRE = Map.of(
            "K-pop", List.of("HYBE", "SM 엔터테인먼트", "코엑스"),
            "K-drama", List.of("북촌 한옥마을", "남산 서울타워", "덕수궁"),
            "K-food", List.of("광장시장", "망원시장", "익선동"),
            "K-beauty", List.of("명동", "가로수길", "성수동"));

    private static final int PER_KEYWORD_ROWS = 10;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final RestClient tourRestClient;

    @Value("${tour.api.base-url}")
    private String baseUrl;

    @Value("${tour.api.service-key}")
    private String serviceKey;

    @Value("${tour.api.mobile-app}")
    private String mobileApp;

    @Cacheable(value = "recommend", key = "#genre + ',' + #lat + ',' + #lng + ',' + #radius")
    public List<RecommendPlaceResponse> findRecommendations(String genre, Double lat, Double lng, Integer radius) {
        validateCoordinatesPairing(lat, lng);
        List<String> genresToSearch = normalizeGenre(genre);

        // contentId 기준 중복 제거
        Map<String, RecommendPlaceResponse> unique = new LinkedHashMap<>();
        for (String g : genresToSearch) {
            for (String keyword : KEYWORDS_BY_GENRE.get(g)) {
                for (JsonNode item : searchTourApi(keyword)) {
                    String contentId = item.path("contentid").asString();
                    if (contentId == null || contentId.isBlank() || unique.containsKey(contentId)) {
                        continue;
                    }
                    RecommendPlaceResponse response = toResponse(item, g, lat, lng);
                    if (response == null) {
                        continue;
                    }
                    if (radius != null && response.distanceMeters() != null && response.distanceMeters() > radius) {
                        continue;
                    }
                    unique.put(contentId, response);
                }
            }
        }

        List<RecommendPlaceResponse> result = new ArrayList<>(unique.values());
        // lat/lng 있으면 거리순, 없으면 삽입 순서 유지
        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(
                    RecommendPlaceResponse::distanceMeters,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        log.info("[Recommend] 조회 완료. genre={}, lat={}, lng={}, radius={}, count={}",
                genre, lat, lng, radius, result.size());
        return result;
    }

    private List<String> normalizeGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return new ArrayList<>(KEYWORDS_BY_GENRE.keySet());
        }
        if (!KEYWORDS_BY_GENRE.containsKey(genre)) {
            throw new RecommendException(RecommendErrorCode.UNSUPPORTED_GENRE);
        }
        return List.of(genre);
    }

    private void validateCoordinatesPairing(Double lat, Double lng) {
        if ((lat == null) ^ (lng == null)) {
            throw new RecommendException(RecommendErrorCode.INCOMPLETE_COORDINATES);
        }
    }

    /**
     * TourAPI searchKeyword2 호출 결과의 item 배열을 List<JsonNode> 로 반환.
     * 실패 시 빈 리스트 (한 키워드 실패가 전체 실패 되지 않게).
     * serviceKey 는 이미 URL 인코딩된 값이라 uriBuilder 대신 URI 문자열 직접 조립.
     */
    private List<JsonNode> searchTourApi(String keyword) {
        String query = "serviceKey=" + serviceKey
                + "&MobileOS=ETC"
                + "&MobileApp=" + mobileApp
                + "&_type=json"
                + "&arrange=A"
                + "&numOfRows=" + PER_KEYWORD_ROWS
                + "&keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "/KorService2/searchKeyword2?" + query);

        JsonNode root;
        try {
            root = tourRestClient.get().uri(uri).retrieve().body(JsonNode.class);
            log.info("[Recommend] TourAPI 검색 성공. keyword={}", keyword);
        } catch (Exception e) {
            log.warn("[Recommend] TourAPI 검색 실패 (계속 진행). keyword={}, message={}", keyword, e.getMessage());
            return List.of();
        }
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

    private RecommendPlaceResponse toResponse(JsonNode node, String genre, Double userLat, Double userLng) {
        Double placeLat = parseDouble(node.path("mapy"));
        Double placeLng = parseDouble(node.path("mapx"));
        if (placeLat == null || placeLng == null) {
            return null;
        }
        Double distance = null;
        if (userLat != null && userLng != null) {
            distance = haversineMeters(userLat, userLng, placeLat, placeLng);
        }
        return new RecommendPlaceResponse(
                node.path("contentid").asString(),
                node.path("contenttypeid").asString(),
                genre,
                node.path("title").asString(),
                placeLat,
                placeLng,
                emptyToNull(node.path("firstimage").asString()),
                emptyToNull(node.path("addr1").asString()),
                distance);
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

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
