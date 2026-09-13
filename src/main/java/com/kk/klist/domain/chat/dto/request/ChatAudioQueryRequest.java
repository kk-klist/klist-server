package com.kk.klist.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChatAudioQueryRequest(
        @NotNull(message = "세션 ID는 필수입니다.")
        String sessionId,

        @Pattern(regexp = "ko|en", message = "언어는 ko 또는 en이어야 합니다.")
        String language
) {
}
