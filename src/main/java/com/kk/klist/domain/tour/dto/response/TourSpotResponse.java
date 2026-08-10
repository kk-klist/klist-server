package com.kk.klist.domain.tour.dto.response;

/**
 * TourAPI nearby / search / festival 응답 항목.
 * distance 는 nearby 응답에만 포함(TourAPI의 dist 필드), 그 외에서는 null.
 */
public record TourSpotResponse(
        String contentId,
        String contentTypeId,
        String title,
        Double latitude,
        Double longitude,
        String imageUrl,
        String address,
        Double distance
) {
}
