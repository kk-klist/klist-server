package com.kk.klist.global.security.config;

import com.kk.klist.global.security.debug.DebugAuthFilter;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * TODO: OAuth2 로그인과 JWT 인증 필터가 들어오기 전까지는 모든 요청을 permitAll()로 열어두고,
     * {@code @LoginUser}가 붙은 API에 한해 {@link LoginUserArgumentResolver}가 401/403을 처리한다.
     * JWT 필터가 준비되면 이 필터 체인을 실제 인가 규칙으로 교체한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Optional<DebugAuthFilter> debugAuthFilter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        debugAuthFilter.ifPresent(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        return http.build();
    }
}
