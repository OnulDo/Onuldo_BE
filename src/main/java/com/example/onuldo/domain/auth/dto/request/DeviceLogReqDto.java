package com.example.onuldo.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DeviceLogReqDto(
        @Schema(example = "device-uuid-1234")
        @NotBlank(message = "deviceId는 필수입니다.")
        String deviceId,

        @Schema(example = "fcm-token-1234", nullable = true)
        String fcmToken
) {
}
