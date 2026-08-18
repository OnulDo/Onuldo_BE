package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EmailLoginReqDto(
        @Schema(example = "onuldo@onuldo.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+_-]+(\\.[a-zA-Z0-9+_-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                        "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email,

        @Schema(example = "12341234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(description = "디바이스 로그 적재를 위한 기기 정보")
        @NotNull(message = "기기 정보는 필수입니다.")
        @Valid
        DeviceLogReqDto device
) {
}
