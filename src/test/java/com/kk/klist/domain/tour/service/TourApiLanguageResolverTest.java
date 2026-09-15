package com.kk.klist.domain.tour.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TourApiLanguageResolverTest {

    private final TourApiLanguageResolver resolver =
            new TourApiLanguageResolver("korean-key", "english-key");

    @Test
    @DisplayName("영어 요청이면 영문 서비스와 영문 키가 선택된다")
    void resolve_whenEnglish_selectsEnglishServiceAndKey() {
        // when
        String service = resolver.resolveService("en");
        String serviceKey = resolver.resolveServiceKey("en");

        // then
        assertThat(service).isEqualTo("EngService2");
        assertThat(serviceKey).isEqualTo("english-key");
    }

    @Test
    @DisplayName("지원하지 않는 언어이면 국문 서비스와 국문 키가 선택된다")
    void resolve_whenUnsupportedLanguage_selectsKoreanServiceAndKey() {
        // when
        String service = resolver.resolveService("ja");
        String serviceKey = resolver.resolveServiceKey("ja");

        // then
        assertThat(service).isEqualTo("KorService2");
        assertThat(serviceKey).isEqualTo("korean-key");
    }
}
