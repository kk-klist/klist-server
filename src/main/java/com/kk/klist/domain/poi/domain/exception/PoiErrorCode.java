package com.kk.klist.domain.poi.domain.exception;

import com.kk.klist.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PoiErrorCode implements ErrorCode {

    KAKAO_LOCAL_API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_LOCAL_API_ERROR", "주변 장소를 불러오지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
