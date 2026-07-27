package com.kk.klist.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kk.klist.global.exception.ErrorCode;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    /**
     * 성공 응답을 생성한다.
     */
    private ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.code = null;
        this.message = null;
        this.errors = null;
    }

    /**
     * 실패 응답을 생성한다.
     */
    private ApiResponse(String code, String message, List<FieldError> errors) {
        this.success = false;
        this.data = null;
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    /**
     * 데이터를 포함하는 성공 응답을 반환한다.
     *
     * @param data 응답 바디에 담을 데이터
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    /**
     * 데이터 없이 성공 응답을 반환한다.
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null);
    }

    /**
     * 필드별 에러 없이 에러 코드만으로 실패 응답을 반환한다.
     *
     * @param errorCode 실패 사유를 나타내는 에러 코드
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 입력값 검증 실패처럼 필드별 에러 목록을 포함하는 실패 응답을 반환한다.
     *
     * @param errorCode 실패 사유를 나타내는 에러 코드
     * @param errors    필드별 검증 에러 목록
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, List<FieldError> errors) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), errors);
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final Object rejectedValue;
        private final String reason;
    }
}
