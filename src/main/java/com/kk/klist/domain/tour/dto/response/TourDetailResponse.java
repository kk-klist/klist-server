package com.kk.klist.domain.tour.dto.response;

import java.util.List;

/**
 * TourAPI detailCommon2 + detailImage2 + detailIntro2 를 합친 상세 응답.
 * useTime 은 contentTypeId 별로 서로 다른 필드(usetime/opentime 등)를 첫 유효값으로 채운다.
 */
public record TourDetailResponse(
        String contentId,
        String contentTypeId,
        String title,
        Double latitude,
        Double longitude,
        String imageUrl,
        String address,
        String overview,
        String useTime,
        List<String> images
) {
}
