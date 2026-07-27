package com.kk.klist.global.security.debug;

import com.kk.klist.global.security.auth.CustomUserDetails;
import com.kk.klist.global.security.auth.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터가 준비되기 전, 로그인 없이도 {@code @LoginUser} 기반 API를 개발/테스트할 수 있도록
 * 요청 헤더로 로그인 사용자를 흉내낸다. JwtAuthenticationFilter가 들어오면 이 클래스는 제거한다.
 */
@Component
@Profile("local")
public class DebugAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-Debug-User-Id";
    private static final String HEADER_ROLE = "X-Debug-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (userIdHeader != null) {
            Long userId = Long.valueOf(userIdHeader);
            Role role = resolveRole(request.getHeader(HEADER_ROLE));
            CustomUserDetails userDetails = new CustomUserDetails(userId, role);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList()));
        }

        filterChain.doFilter(request, response);
    }

    private Role resolveRole(String roleHeader) {
        if (roleHeader == null) {
            return Role.USER;
        }
        return Role.valueOf(roleHeader);
    }
}
