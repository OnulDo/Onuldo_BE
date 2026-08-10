package com.example.onuldo.global.security;

import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// authorizeHttpRequests가 인증되지 않은 요청을 거부할 때 호출된다.
// JwtAuthenticationFilter가 미리 담아둔 구체적인 에러 코드(만료/위조 토큰 등)가 있으면 그걸 쓰고,
// 없으면(토큰 자체가 없는 경우) 기본 _UNAUTHORIZED로 응답해, 기존 ExceptionAdvice와 동일한
// BaseResponse 포맷을 유지한다.
// 이 프로젝트엔 전역 ObjectMapper 빈이 없어(ChallengeService, DiscordAlertSender와 동일 컨벤션)
// 직접 생성해서 쓴다. BaseResponse.timestamp(LocalDateTime) 직렬화를 위해 JavaTimeModule만 등록.
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        BaseCodeDto errorCode = resolveErrorCode(request);

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        BaseResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), null)
                )
        );
    }

    private BaseCodeDto resolveErrorCode(HttpServletRequest request) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (attribute instanceof BaseCodeDto errorCode) {
            return errorCode;
        }
        return GlobalErrorStatus._UNAUTHORIZED.getCode();
    }
}
