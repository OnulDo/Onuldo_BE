package com.example.onuldo.global.security;

import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.UserStatus;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// SecurityConfig의 authorizeHttpRequests(.authenticated())가 실제로 판단할 수 있도록
// Bearer 토큰을 검증해 SecurityContext에 Authentication을 채워 넣는다.
// 토큰이 없거나 유효하지 않아도 여기서 직접 막지 않고 그대로 통과시켜, 이후 authorizeHttpRequests +
// JwtAuthenticationEntryPoint가 permitAll 여부에 따라 최종 판단하도록 위임한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String AUTH_ERROR_ATTRIBUTE = "authError";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
            try {
                Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
                User user = userRepository.findById(userId).orElse(null);

                if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorStatus._USER_NOT_FOUND);
                } else {
                    authenticate(request, userId);
                }
            } catch (RestApiException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Long userId) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, userId);
    }
}
