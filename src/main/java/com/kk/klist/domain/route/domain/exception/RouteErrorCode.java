package com.kk.klist.domain.route.domain.exception;

import com.kk.klist.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RouteErrorCode implements ErrorCode {

    KAKAO_MOBILITY_API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_MOBILITY_API_ERROR", "길찾기 경로를 불러오지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
