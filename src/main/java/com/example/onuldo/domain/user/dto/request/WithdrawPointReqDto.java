package com.example.onuldo.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record WithdrawPointReqDto (
        @Schema(example = "10000")
        @NotNull(message = "출금 포인트 금액은 필수입니다.")
        @Positive(message = "출금 포인트 금액은 0보다 커야 합니다.")
        Integer point
){
}
