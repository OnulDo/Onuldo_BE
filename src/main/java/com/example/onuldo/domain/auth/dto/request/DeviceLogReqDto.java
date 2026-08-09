package com.example.onuldo.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record DeviceLogReqDto(
        @Schema(example = "device-uuid-1234")
        @NotBlank(message = "deviceId는 필수입니다.")
        @Size(max = 255, message = "deviceId는 255자 이하여야 합니다.")
        String deviceId,

        @Schema(example = "fcm-token-1234", nullable = true)
        @Size(min = 1, max = 512, message = "fcmToken은 1자 이상 512자 이하여야 합니다.")
        String fcmToken
) {
}
