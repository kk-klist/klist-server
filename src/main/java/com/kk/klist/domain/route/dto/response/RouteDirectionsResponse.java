package com.kk.klist.domain.route.dto.response;

import java.util.List;

/**
 * 카카오모빌리티 Directions 결과.
 * path — 폴리라인용 좌표 배열, distanceMeters — 총 거리(m), durationSeconds — 예상 소요(초).
 * 결과 경로 없으면 path 는 빈 리스트, distance/duration 은 null.
 */
public record RouteDirectionsResponse(
        List<LatLng> path,
        Integer distanceMeters,
        Integer durationSeconds
) {

    public record LatLng(double lat, double lng) {
    }
}
