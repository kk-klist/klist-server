package com.kk.klist.global.security.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 사용자의 식별자를 받는 Controller 파라미터에 붙인다.
 * {@code @LoginUser Long userId} 형태로 사용한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

    /**
     * true면 인증되지 않은 요청에 대해 {@code AuthErrorCode.UNAUTHORIZED} 예외를 던진다.
     */
    boolean required() default true;

    /**
     * 비어있지 않으면 로그인한 사용자의 역할이 이 목록에 포함되어야 하며,
     * 아니면 {@code AuthErrorCode.FORBIDDEN} 예외를 던진다.
     */
    Role[] requiredRoles() default {};
}
