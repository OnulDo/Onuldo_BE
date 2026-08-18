package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GetProfileResDto(
        @Schema(example = "오늘두")
        String nickname,
        @Schema(example = "onuldo@onuldo.com")
        String email,
        @Schema(example = "https://cdn.onuldo.com/profile/default.png", nullable = true)
        String profileImageUrl
) {
}
