package com.kk.klist.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
