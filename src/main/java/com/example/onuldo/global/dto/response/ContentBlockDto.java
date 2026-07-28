package com.example.onuldo.global.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ContentBlockDto(
        @Schema(example = "h2")
        String type,
        @Schema(example = "안녕하세요 반가워요")
        String content
) {
}
