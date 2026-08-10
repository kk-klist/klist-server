package com.kk.klist.domain.tour.controller;

import com.kk.klist.domain.tour.dto.response.TourDetailResponse;
import com.kk.klist.domain.tour.dto.response.TourSpotResponse;
import com.kk.klist.domain.tour.service.TourService;
import com.kk.klist.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tour")
@RequiredArgsConstructor
public class TourController {

    private static final int DEFAULT_RADIUS_METERS = 2000;
    private static final String DEFAULT_LANG = "ko";

    private final TourService tourService;

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<TourSpotResponse>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "" + DEFAULT_RADIUS_METERS) int radius,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = DEFAULT_LANG) String lang
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tourService.findNearby(lat, lng, radius, category, lang)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TourSpotResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = DEFAULT_LANG) String lang
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tourService.search(keyword, lang)));
    }

    @GetMapping("/festival")
    public ResponseEntity<ApiResponse<List<TourSpotResponse>>> getFestivals(
            @RequestParam(required = false) String eventStartDate,
            @RequestParam(required = false) String areaCode,
            @RequestParam(defaultValue = DEFAULT_LANG) String lang
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tourService.findFestivals(eventStartDate, areaCode, lang)));
    }

    @GetMapping("/detail/{contentId}")
    public ResponseEntity<ApiResponse<TourDetailResponse>> getDetail(
            @PathVariable String contentId,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(defaultValue = DEFAULT_LANG) String lang
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tourService.findDetail(contentId, contentTypeId, lang)));
    }
}
