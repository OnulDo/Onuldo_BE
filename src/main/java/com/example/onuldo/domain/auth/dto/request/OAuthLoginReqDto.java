package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginReqDto(
        @Schema(example = "KAKAO")
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @Schema(example = "string")
        @NotBlank(message = "socialAccessToken은 필수입니다.")
        String socialAccessToken,

        @Schema(description = "디바이스 로그 적재를 위한 기기 정보")
        @NotNull(message = "기기 정보는 필수입니다.")
        @Valid
        DeviceLogReqDto device
) {
}
