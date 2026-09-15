package com.kk.klist.domain.recommend.service;

import com.kk.klist.domain.recommend.domain.exception.RecommendErrorCode;
import com.kk.klist.domain.recommend.domain.exception.RecommendException;
import com.kk.klist.domain.recommend.dto.response.RecommendPlaceResponse;
import com.kk.klist.domain.tour.domain.exception.TourException;
import com.kk.klist.domain.tour.dto.response.TourSpotResponse;
import com.kk.klist.domain.tour.service.TourService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * K-컬처 장르별 추천 — 공모전 필수 데이터인 한국관광공사 TourAPI(searchKeyword2)로 큐레이션한다.
 * 각 장르는 K-컬처 대표 명소 키워드 목록을 갖고, 키워드별 검색 결과를 합쳐 dedup+거리 정렬 후 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    // 장르별 TourAPI 검색 키워드 (큐레이션). 한 장르에 여러 키워드로 검색해 결과 합침.
    // ⚠ TourAPI 에 실제로 등록된 키워드만 쓴다. "HYBE"/"SM 엔터테인먼트" 는 검색 결과가 0건이라
    //   K-pop 장르가 전 지역에서 비어 보였다. 공연·이벤트 장소 위주로 교체했다.
    private static final Map<String, List<String>> KOREAN_KEYWORDS_BY_GENRE = Map.of(
            "K-pop", List.of("코엑스", "장충체육관", "동대문디자인플라자", "케이팝"),
            "K-drama", List.of("북촌 한옥마을", "남산 서울타워", "덕수궁"),
            "K-food", List.of("광장시장", "망원시장", "익선동"),
            "K-beauty", List.of("명동", "가로수길", "성수동"));
    private static final Map<String, List<String>> ENGLISH_KEYWORDS_BY_GENRE = Map.of(
            "K-pop", List.of("COEX", "Jangchung Arena", "Dongdaemun Design Plaza", "K-pop"),
            "K-drama", List.of("Bukchon Hanok Village", "N Seoul Tower", "Deoksugung Palace"),
            "K-food", List.of("Gwangjang Market", "Mangwon Market", "Ikseon-dong"),
            "K-beauty", List.of("Myeong-dong", "Garosu-gil", "Seongsu-dong"));
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final TourService tourService;

    @Cacheable(value = "recommend", key = "#genre + ',' + #lat + ',' + #lng + ',' + #radius + ',' + #lang")
    public List<RecommendPlaceResponse> findRecommendations(
            String genre, Double lat, Double lng, Integer radius, String lang) {
        validateCoordinatesPairing(lat, lng);
        List<String> genresToSearch = normalizeGenre(genre);
        String resolvedLanguage = resolveLanguage(lang);
        Map<String, List<String>> keywordsByGenre = keywordsByGenre(resolvedLanguage);

        // contentId 기준 중복 제거
        Map<String, RecommendPlaceResponse> unique = new LinkedHashMap<>();
        for (String g : genresToSearch) {
            for (String keyword : keywordsByGenre.get(g)) {
                for (TourSpotResponse item : searchTourApi(keyword, resolvedLanguage)) {
                    String contentId = item.contentId();
                    if (contentId == null || contentId.isBlank() || unique.containsKey(contentId)) {
                        continue;
                    }
                    RecommendPlaceResponse response = toResponse(item, g, lat, lng);
                    if (response == null) {
                        continue;
                    }
                    if (radius != null && response.distanceMeters() != null && response.distanceMeters() > radius) {
                        continue;
                    }
                    unique.put(contentId, response);
                }
            }
        }

        List<RecommendPlaceResponse> result = new ArrayList<>(unique.values());
        // lat/lng 있으면 거리순, 없으면 삽입 순서 유지
        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(
                    RecommendPlaceResponse::distanceMeters,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        log.info("[Recommend] 조회 완료. genre={}, lat={}, lng={}, radius={}, lang={}, count={}",
                genre, lat, lng, radius, resolvedLanguage, result.size());
        return result;
    }

    private List<String> normalizeGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return new ArrayList<>(KOREAN_KEYWORDS_BY_GENRE.keySet());
        }
        if (!KOREAN_KEYWORDS_BY_GENRE.containsKey(genre)) {
            throw new RecommendException(RecommendErrorCode.UNSUPPORTED_GENRE);
        }
        return List.of(genre);
    }

    private void validateCoordinatesPairing(Double lat, Double lng) {
        if ((lat == null) ^ (lng == null)) {
            throw new RecommendException(RecommendErrorCode.INCOMPLETE_COORDINATES);
        }
    }

    private List<TourSpotResponse> searchTourApi(String keyword, String language) {
        try {
            return tourService.search(keyword, language);
        } catch (TourException e) {
            return List.of();
        }
    }

    private RecommendPlaceResponse toResponse(
            TourSpotResponse node, String genre, Double userLat, Double userLng) {
        Double placeLat = node.latitude();
        Double placeLng = node.longitude();
        if (placeLat == null || placeLng == null) {
            return null;
        }
        Double distance = null;
        if (userLat != null && userLng != null) {
            distance = haversineMeters(userLat, userLng, placeLat, placeLng);
        }
        return new RecommendPlaceResponse(
                node.contentId(),
                node.contentTypeId(),
                genre,
                node.title(),
                placeLat,
                placeLng,
                node.imageUrl(),
                node.address(),
                distance);
    }

    private String resolveLanguage(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "ko";
    }

    private Map<String, List<String>> keywordsByGenre(String language) {
        return "en".equals(language) ? ENGLISH_KEYWORDS_BY_GENRE : KOREAN_KEYWORDS_BY_GENRE;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
