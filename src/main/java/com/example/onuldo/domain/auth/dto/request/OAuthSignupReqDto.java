package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record OAuthSignupReqDto(
        @Schema(example = "KAKAO")
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @Schema(example = "string")
        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken,

        @Schema(example = "오늘두")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @Schema(example = "https://cdn.onuldo.com/profile/default.png", nullable = true)
        String profileImageUrl,

        @Schema(description = "약관 동의 목록")
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        @Valid
        List<TermAgreementReqDto> termAgreements
) {
}
