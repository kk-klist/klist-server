package com.kk.klist.domain.recommend.controller;

import com.kk.klist.domain.recommend.dto.response.RecommendPlaceResponse;
import com.kk.klist.domain.recommend.service.RecommendService;
import com.kk.klist.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendPlaceResponse>>> getRecommendations(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendService.findRecommendations(genre, lat, lng, radius)));
    }
}
