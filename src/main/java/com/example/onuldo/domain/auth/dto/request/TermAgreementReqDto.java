package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.auth.enums.TermType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TermAgreementReqDto(
        @NotNull(message = "약관 ID는 필수입니다.")
        TermType termType,

        @NotNull(message = "약관 동의 값은 필수입니다.")
        Boolean value
) {
}
