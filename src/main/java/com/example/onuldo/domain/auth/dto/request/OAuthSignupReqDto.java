package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OAuthSignupReqDto(
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        String profileImageUrl,

        @NotNull(message = "약관 동의 목록은 필수입니다.")
        @Valid
        List<TermAgreementReqDto> termAgreements
) {
}
