package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RefreshTokenReqDto(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9")
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken,

        @Schema(description = "디바이스 로그 적재를 위한 기기 정보")
        @NotNull(message = "기기 정보는 필수입니다.")
        @Valid
        DeviceLogReqDto device
) {
}
