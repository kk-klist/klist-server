package com.kk.klist.domain.route.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RouteDirectionsRequest(
        @NotNull(message = "출발지 위도는 필수입니다.")
        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        Double originLat,

        @NotNull(message = "출발지 경도는 필수입니다.")
        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        Double originLng,

        @NotNull(message = "도착지 위도는 필수입니다.")
        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        Double destLat,

        @NotNull(message = "도착지 경도는 필수입니다.")
        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        Double destLng
) {
}
