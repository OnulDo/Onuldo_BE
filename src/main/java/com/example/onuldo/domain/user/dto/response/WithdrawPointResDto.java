package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record WithdrawPointResDto (
        @Schema(example = "10000")
        Integer amount,
        @Schema(example = "0")
        Long balanceAfter
){
}
