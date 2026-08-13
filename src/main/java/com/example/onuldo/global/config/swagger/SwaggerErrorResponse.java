package com.example.onuldo.global.config.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공통 에러 응답")
public record SwaggerErrorResponse(
        @Schema(example = "2026-08-13T12:00:00")
        LocalDateTime timestamp,

        @Schema(example = "BAD_REQUEST")
        String code,

        @Schema(example = "잘못된 요청입니다.")
        String message,

        @Schema(nullable = true, description = "검증 오류 상세 또는 포인트 부족 상세. 없으면 null")
        Object result
) {
}
