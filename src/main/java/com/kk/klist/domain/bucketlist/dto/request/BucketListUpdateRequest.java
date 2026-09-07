package com.kk.klist.domain.bucketlist.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BucketListUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,

        @Size(max = 10000, message = "설명은 10000자 이하로 입력해주세요.")
        String description,

        @NotBlank(message = "카테고리는 필수입니다.")
        String category,

        @Size(max = 255, message = "장소명은 255자 이하로 입력해주세요.")
        String placeName,

        @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
        String address,

        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude,

        @Size(max = 2048, message = "이미지 URL은 2048자 이하로 입력해주세요.")
        String imageUrl
) {
}
