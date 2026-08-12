package com.example.onuldo.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;
import lombok.Builder;

import java.util.List;

@Builder
public record EmailSignupReqDto(
        @Schema(example = "onuldo@onuldo.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+_-]+(\\.[a-zA-Z0-9+_-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                        "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email,

        @Schema(example = "onuldo1234!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(example = "오늘두")
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @Schema(example = "https://onuldo-bucket.s3.ap-northeast-2.amazonaws.com/profile/1.png")
        @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
        String profileImageUrl,

        @Schema(
                description = "약관 동의 목록",
                example = "[{\"termType\":\"SERVICE\",\"value\":true}," +
                        "{\"termType\":\"PRIVACY\",\"value\":true}," +
                        "{\"termType\":\"AGE_14\",\"value\":true}," +
                        "{\"termType\":\"REFUND\",\"value\":true}]"
        )
        @NotNull(message = "약관 동의 목록은 필수입니다.")
        List<@NotNull @Valid TermAgreementReqDto> termAgreements,

        @Schema(description = "디바이스 로그 적재를 위한 기기 정보")
        @NotNull(message = "기기 정보는 필수입니다.")
        @Valid
        DeviceLogReqDto device
) {
}
