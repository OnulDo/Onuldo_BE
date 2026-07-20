package com.example.onuldo.domain.auth.service.client.dto;

import com.example.onuldo.domain.user.enums.SocialProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserInfoResDto(
        String resultcode,
        String message,
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            String id,
            String email,
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) {
    }

    public OAuthUserInfo toOAuthUserInfo() {
        return OAuthUserInfo.builder()
                .provider(SocialProvider.NAVER)
                .socialId(response.id())
                .email(response.email())
                .nickname(response.nickname())
                .profileImageUrl(response.profileImage())
                .build();
    }
}
