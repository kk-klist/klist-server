package com.kk.klist.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoApiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient kakaoLocalRestClient(@Value("${kakao.local.base-url}") String baseUrl) {
        return buildKakaoRestClient(baseUrl);
    }

    @Bean
    public RestClient kakaoMobilityRestClient(@Value("${kakao.mobility.base-url}") String baseUrl) {
        return buildKakaoRestClient(baseUrl);
    }

    private RestClient buildKakaoRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
