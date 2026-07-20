package com.example.onuldo.domain.auth.service.client.dto;

import com.example.onuldo.domain.user.enums.SocialProvider;
import lombok.Builder;

@Builder
public record OAuthUserInfo(
        SocialProvider provider,
        String socialId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
