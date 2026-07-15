package com.example.onuldo.domain.auth.dto.response;

import lombok.Builder;

@Builder
public record AuthResDto(
        String accessToken,
        String refreshToken
) {
}
