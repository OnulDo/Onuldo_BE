package com.example.onuldo.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record OAuthResDto(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(example = "true")
        boolean isNewUser
) {
}
