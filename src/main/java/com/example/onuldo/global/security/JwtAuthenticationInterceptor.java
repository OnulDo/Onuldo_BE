package com.example.onuldo.global.security;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.domain.user.enums.UserStatus;
import com.example.onuldo.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        if (userRepository.findById(userId)
                .map(user -> user.getStatus() != UserStatus.ACTIVE)
                .orElse(true)) {
            throw new RestApiException(GlobalErrorStatus._USER_NOT_FOUND);
        }
        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
