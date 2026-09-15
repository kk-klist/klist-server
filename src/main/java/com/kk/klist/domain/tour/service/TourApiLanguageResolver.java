package com.kk.klist.domain.tour.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TourApiLanguageResolver {

    private final String koreanServiceKey;
    private final String englishServiceKey;

    public TourApiLanguageResolver(
            @Value("${tour.api.service-key.ko}") String koreanServiceKey,
            @Value("${tour.api.service-key.en}") String englishServiceKey
    ) {
        this.koreanServiceKey = koreanServiceKey;
        this.englishServiceKey = englishServiceKey;
    }

    public String resolveLanguage(String language) {
        return "en".equalsIgnoreCase(language) ? "en" : "ko";
    }

    public String resolveService(String language) {
        return "en".equals(resolveLanguage(language)) ? "EngService2" : "KorService2";
    }

    public String resolveServiceKey(String language) {
        return "en".equals(resolveLanguage(language)) ? englishServiceKey : koreanServiceKey;
    }
}
