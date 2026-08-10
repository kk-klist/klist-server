package com.kk.klist.domain.route.controller;

import com.kk.klist.domain.route.dto.request.RouteDirectionsRequest;
import com.kk.klist.domain.route.dto.response.RouteDirectionsResponse;
import com.kk.klist.domain.route.service.RouteService;
import com.kk.klist.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // 경로 계산은 리소스 생성이 아니므로 200 OK 로 응답
    @PostMapping("/directions")
    public ResponseEntity<ApiResponse<RouteDirectionsResponse>> findDirections(
            @Valid @RequestBody RouteDirectionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                routeService.findDirections(request)));
    }
}
