package com.kk.klist.domain.bucketlist.dto.request;

import jakarta.validation.constraints.NotNull;

public record BucketListCompletionUpdateRequest(
        @NotNull(message = "완료 여부는 필수입니다.")
        Boolean isCompleted
) {
}
