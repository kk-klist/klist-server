package com.kk.klist.domain.route.service;

import com.kk.klist.domain.route.domain.exception.RouteErrorCode;
import com.kk.klist.domain.route.domain.exception.RouteException;
import com.kk.klist.domain.route.dto.request.RouteDirectionsRequest;
import com.kk.klist.domain.route.dto.response.RouteDirectionsResponse;
import com.kk.klist.domain.route.dto.response.RouteDirectionsResponse.LatLng;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * 카카오모빌리티 자동차 길찾기(Directions) 프록시.
 * Kakao API 는 origin/destination 을 "경도,위도" 순서 쿼리 파라미터로 요구하므로
 * URI 를 직접 조립한다 (RestClient uriBuilder 로 넘기면 콤마가 %2C 로 인코딩되어 실패 가능).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private static final String DIRECTIONS_PATH = "/v1/directions";
    private static final String DEFAULT_PRIORITY = "RECOMMEND";

    private final RestClient kakaoMobilityRestClient;

    @Value("${kakao.rest-key}")
    private String restKey;

    public RouteDirectionsResponse findDirections(RouteDirectionsRequest request) {
        URI uri = URI.create(DIRECTIONS_PATH
                + "?origin=" + request.originLng() + "," + request.originLat()
                + "&destination=" + request.destLng() + "," + request.destLat()
                + "&priority=" + DEFAULT_PRIORITY);

        JsonNode root;
        try {
            root = kakaoMobilityRestClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + restKey)
                    .retrieve()
                    .body(JsonNode.class);
            log.info("[Route] 경로 조회 성공. origin=({},{}), dest=({},{})",
                    request.originLat(), request.originLng(), request.destLat(), request.destLng());
        } catch (Exception e) {
            log.warn("[Route] 경로 조회 실패. message={}", e.getMessage());
            throw new RouteException(RouteErrorCode.KAKAO_MOBILITY_API_ERROR);
        }

        return parseDirections(root);
    }

    private RouteDirectionsResponse parseDirections(JsonNode root) {
        List<LatLng> path = new ArrayList<>();
        if (root == null) {
            return new RouteDirectionsResponse(path, null, null);
        }

        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) {
            return new RouteDirectionsResponse(path, null, null);
        }

        JsonNode firstRoute = routes.get(0);
        JsonNode summary = firstRoute.path("summary");
        Integer distanceMeters = summary.path("distance").isMissingNode() ? null : summary.path("distance").asInt();
        Integer durationSeconds = summary.path("duration").isMissingNode() ? null : summary.path("duration").asInt();

        // vertexes 는 [lng, lat, lng, lat, ...] 로 평면화된 배열
        for (JsonNode section : firstRoute.path("sections")) {
            for (JsonNode road : section.path("roads")) {
                JsonNode vertexes = road.path("vertexes");
                for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                    double lng = vertexes.get(i).asDouble();
                    double lat = vertexes.get(i + 1).asDouble();
                    path.add(new LatLng(lat, lng));
                }
            }
        }

        log.info("[Route] 경로 파싱 완료. distanceMeters={}, durationSeconds={}, points={}",
                distanceMeters, durationSeconds, path.size());
        return new RouteDirectionsResponse(path, distanceMeters, durationSeconds);
    }
}
