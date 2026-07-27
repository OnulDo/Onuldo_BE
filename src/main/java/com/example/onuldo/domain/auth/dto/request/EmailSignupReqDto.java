package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record EmailSignupReqDto(
        @Schema(example = "onuldo@onuldo.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+\\-_\\.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email,

        @Schema(example = "12341234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

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
