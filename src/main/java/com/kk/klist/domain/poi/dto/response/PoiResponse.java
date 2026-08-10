package com.kk.klist.domain.poi.dto.response;

/**
 * 카카오 로컬 category.json 응답 항목.
 * category_group_code 로 그룹 검색이라 개별 항목의 categoryCode 는 응답에 없으니 제외.
 */
public record PoiResponse(
        String id,
        String placeName,
        String address,
        Double latitude,
        Double longitude,
        Double distance
) {
}
