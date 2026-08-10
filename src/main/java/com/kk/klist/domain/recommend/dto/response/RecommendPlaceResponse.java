package com.kk.klist.domain.recommend.dto.response;

/**
 * 장르별 K-컬처 추천 장소. TourAPI searchKeyword2 응답을 매핑.
 * distanceMeters 는 요청에 lat/lng 준 경우만 계산 (아니면 null).
 */
public record RecommendPlaceResponse(
        String contentId,
        String contentTypeId,
        String genre,
        String title,
        Double latitude,
        Double longitude,
        String imageUrl,
        String address,
        Double distanceMeters
) {
}
