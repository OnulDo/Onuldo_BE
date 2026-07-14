package com.example.onuldo.global.security;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
