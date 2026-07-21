package com.example.onuldo.domain.auth.service.client.dto;

import com.example.onuldo.domain.user.enums.SocialProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResDto(
        Long id,
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email
    ) {
    }

    public OAuthUserInfo toOAuthUserInfo() {
        String email = kakaoAccount != null ? kakaoAccount.email() : null;

        return OAuthUserInfo.builder()
                .provider(SocialProvider.KAKAO)
                .socialId(String.valueOf(id))
                .email(email)
                .build();
    }
}
