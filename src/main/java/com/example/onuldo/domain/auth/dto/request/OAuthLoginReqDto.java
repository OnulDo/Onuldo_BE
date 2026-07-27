package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginReqDto(
        @Schema(example = "KAKAO")
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @Schema(example = "string")
        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken
) {
}
