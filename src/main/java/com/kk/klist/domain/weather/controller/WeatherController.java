package com.kk.klist.domain.weather.controller;

import com.kk.klist.domain.weather.dto.response.WeatherOutfitResponse;
import com.kk.klist.domain.weather.service.WeatherOutfitService;
import com.kk.klist.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private static final String DEFAULT_LANG = "ko";

    private final WeatherOutfitService weatherOutfitService;

    @GetMapping("/outfit")
    public ApiResponse<WeatherOutfitResponse> getOutfit(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = DEFAULT_LANG) String lang
    ) {
        return ApiResponse.success(weatherOutfitService.getWeatherOutfit(latitude, longitude, lang));
    }
}
