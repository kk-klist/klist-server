package com.kk.klist.global.security.auth;

import lombok.Getter;

/**
 * 인증된 사용자를 나타내는 Authentication principal.
 * 인증 방식(임시 헤더 기반, 추후 JWT)이 바뀌어도 이 타입만 SecurityContext에 채우면
 * {@link LoginUserArgumentResolver} 이후 계층은 변경 없이 동작한다.
 */
@Getter
public class CustomUserDetails {

    private final Long userId;
    private final Role role;

    public CustomUserDetails(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }
}
