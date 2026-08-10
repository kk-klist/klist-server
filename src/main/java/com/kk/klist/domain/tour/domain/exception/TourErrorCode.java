package com.kk.klist.domain.tour.domain.exception;

import com.kk.klist.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TourErrorCode implements ErrorCode {

    TOUR_API_ERROR(HttpStatus.BAD_GATEWAY, "TOUR_API_ERROR", "관광 정보를 불러오지 못했습니다."),
    TOUR_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "TOUR_DETAIL_NOT_FOUND", "요청한 장소 상세 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
