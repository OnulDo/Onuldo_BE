package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record EmailLoginReqDto(
        @Schema(example = "onuldo@onuldo.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+\\-_\\.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email,

        @Schema(example = "12341234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
