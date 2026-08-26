package com.kk.klist.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PreferredLanguageUpdateRequest(

        @NotBlank(message = "언어 코드는 필수입니다.")
        @Pattern(
                regexp = "^(ko|en|ja|zh-CN|zh-TW|ru|es|de|fr)$",
                message = "지원하지 않는 언어입니다."
        )
        String preferredLanguage
) {}
