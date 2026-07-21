package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthReqDto(
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken,

        String nickname,

        String profileImageUrl
) {
}