package com.kk.klist.domain.poi.controller;

import com.kk.klist.domain.poi.dto.response.PoiResponse;
import com.kk.klist.domain.poi.service.PoiService;
import com.kk.klist.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/poi")
@RequiredArgsConstructor
public class PoiController {

    private static final int DEFAULT_RADIUS_METERS = 1000;

    private final PoiService poiService;

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<PoiResponse>>> getByCategory(
            @RequestParam String code,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "" + DEFAULT_RADIUS_METERS) int radius
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                poiService.findByCategory(code, lat, lng, radius)));
    }
}
