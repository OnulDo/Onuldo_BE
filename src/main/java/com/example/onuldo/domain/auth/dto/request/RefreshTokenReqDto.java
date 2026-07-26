package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RefreshTokenReqDto(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
