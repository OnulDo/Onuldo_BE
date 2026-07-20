package com.example.onuldo.domain.auth.dto.response;

import lombok.Builder;

@Builder
public record OAuthResDto(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
