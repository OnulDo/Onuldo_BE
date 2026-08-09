package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OAuthSignupReqDto(
        @Schema(example = "KAKAO")
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @Schema(example = "AbCdEfGhIjKlMnOpQrStUvWxYz")
        @NotBlank(message = "socialAccessToken은 필수입니다.")
        String socialAccessToken,

        @Schema(example = "오늘두")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @Schema(example = "https://cdn.onuldo.com/profile/default.png")
        @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
        String profileImageUrl,

        @Schema(
                description = "약관 동의 목록",
                example = "[{\"termType\":\"SERVICE\",\"value\":true}," +
                        "{\"termType\":\"PRIVACY\",\"value\":true}," +
                        "{\"termType\":\"AGE_14\",\"value\":true}," +
                        "{\"termType\":\"MARKETING\",\"value\":false}]"
        )
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        List<@NotNull @Valid TermAgreementReqDto> termAgreements,

        @Schema(description = "디바이스 로그 적재를 위한 기기 정보")
        @NotNull(message = "기기 정보는 필수입니다.")
        @Valid
        DeviceLogReqDto device
) {
}
