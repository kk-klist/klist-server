package com.kk.klist.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

import com.kk.klist.domain.recommend.dto.response.RecommendPlaceResponse;
import com.kk.klist.domain.tour.dto.response.TourSpotResponse;
import com.kk.klist.domain.tour.service.TourService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @InjectMocks
    private RecommendService recommendService;

    @Mock
    private TourService tourService;

    @Test
    @DisplayName("영어 추천 요청이면 영문 키워드와 영어 언어로 관광지를 검색한다")
    void findRecommendations_whenEnglish_searchesWithEnglishKeywordAndLanguage() {
        // given
        TourSpotResponse spot = new TourSpotResponse(
                "1", "12", "COEX", 37.511, 127.059, null, "Seoul", null);
        given(tourService.search("COEX", "en")).willReturn(List.of(spot));

        // when
        List<RecommendPlaceResponse> result =
                recommendService.findRecommendations("K-pop", 37.5, 127.0, null, "en");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("COEX");
        then(tourService).should(atLeastOnce()).search("COEX", "en");
    }
}
