package com.kk.klist.domain.poi.service;

import com.kk.klist.domain.poi.domain.exception.PoiErrorCode;
import com.kk.klist.domain.poi.domain.exception.PoiException;
import com.kk.klist.domain.poi.dto.response.PoiResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * 카카오 로컬 카테고리 그룹코드 기반 생활 POI 검색.
 * 지원 코드: FD6(음식점), CE7(카페), BK9(은행), CS2(편의점), PM9(주차장) 등.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {

    private static final String CATEGORY_SEARCH_PATH = "/v2/local/search/category.json";
    private static final int MAX_RADIUS_METERS = 20_000;
    private static final int RESULT_SIZE = 15;

    private final RestClient kakaoLocalRestClient;

    @Value("${kakao.rest-key}")
    private String restKey;

    @Cacheable(value = "poiCategory", key = "#code + ',' + #lat + ',' + #lng + ',' + #radius")
    public List<PoiResponse> findByCategory(String code, double lat, double lng, int radius) {
        int effectiveRadius = Math.min(radius, MAX_RADIUS_METERS);

        JsonNode root;
        try {
            root = kakaoLocalRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CATEGORY_SEARCH_PATH)
                            .queryParam("category_group_code", code)
                            .queryParam("x", lng)
                            .queryParam("y", lat)
                            .queryParam("radius", effectiveRadius)
                            .queryParam("sort", "distance")
                            .queryParam("size", RESULT_SIZE)
                            .build())
                    .header("Authorization", "KakaoAK " + restKey)
                    .retrieve()
                    .body(JsonNode.class);
            log.info("[Poi] 카테고리 조회 성공. code={}, lat={}, lng={}, radius={}", code, lat, lng, effectiveRadius);
        } catch (Exception e) {
            log.warn("[Poi] 카테고리 조회 실패. code={}, message={}", code, e.getMessage());
            throw new PoiException(PoiErrorCode.KAKAO_LOCAL_API_ERROR);
        }

        if (root == null) {
            throw new PoiException(PoiErrorCode.KAKAO_LOCAL_API_ERROR);
        }

        List<PoiResponse> result = new ArrayList<>();
        for (JsonNode doc : root.path("documents")) {
            result.add(toPoi(doc));
        }
        log.info("[Poi] 카테고리 조회 완료. code={}, count={}", code, result.size());
        return result;
    }

    private PoiResponse toPoi(JsonNode doc) {
        String address = doc.path("road_address_name").asString();
        if (address == null || address.isBlank()) {
            address = doc.path("address_name").asString();
        }
        return new PoiResponse(
                doc.path("id").asString(),
                doc.path("place_name").asString(),
                address,
                parseDouble(doc.path("y")),
                parseDouble(doc.path("x")),
                parseDouble(doc.path("distance"))
        );
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
}
