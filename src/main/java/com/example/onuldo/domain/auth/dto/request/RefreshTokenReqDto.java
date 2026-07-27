package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RefreshTokenReqDto(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9")
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
