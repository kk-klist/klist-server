package com.kk.klist.global.security.auth;

import com.kk.klist.global.exception.AuthErrorCode;
import com.kk.klist.global.exception.AuthException;
import java.util.Arrays;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        LoginUser loginUser = parameter.getParameterAnnotation(LoginUser.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            if (loginUser.required()) {
                throw new AuthException(AuthErrorCode.UNAUTHORIZED);
            }
            return null;
        }

        Role[] requiredRoles = loginUser.requiredRoles();
        if (requiredRoles.length > 0 && Arrays.stream(requiredRoles).noneMatch(role -> role == userDetails.getRole())) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }

        return userDetails.getUserId();
    }
}
