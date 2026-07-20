package com.example.onuldo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import lombok.Builder;

import java.util.List;

@Builder
public record EmailSignupReqDto(
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+\\-_\\.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        String profileImageUrl,

        @NotNull(message = "약관 동의 목록은 필수입니다.")
        @Valid
        List<TermAgreementReqDto> termAgreements
) {
}
