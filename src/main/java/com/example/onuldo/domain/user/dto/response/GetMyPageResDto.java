package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetMyPageResDto(
        @Schema(example = "오늘두")
        String nickname,
        @Schema(example = "onuldo@onuldo.com")
        String email,
        @Schema(example = "https://cdn.onuldo.com/profile/default.png", nullable = true)
        String profileImageUrl,
        @Schema(example = "15000")
        Long currentPoint,
        @Schema(example = "2026-07-24")
        LocalDate joinedAt
) {
}
