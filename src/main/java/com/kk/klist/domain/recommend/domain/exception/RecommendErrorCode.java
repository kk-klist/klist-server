package com.kk.klist.domain.recommend.domain.exception;

import com.kk.klist.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendErrorCode implements ErrorCode {

    INCOMPLETE_COORDINATES(HttpStatus.BAD_REQUEST, "INCOMPLETE_COORDINATES", "위도와 경도를 모두 입력해주세요."),
    UNSUPPORTED_GENRE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_GENRE", "지원하지 않는 장르입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
