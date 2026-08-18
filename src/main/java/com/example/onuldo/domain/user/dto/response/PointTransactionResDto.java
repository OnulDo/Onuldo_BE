package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.PointTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PointTransactionResDto (
        @Schema(example = "REFUND")
        PointTransactionType type,
        @Schema(example = "환급")
        String title,
        @Schema(example = "5000")
        Integer amount,
        @Schema(example = "5000")
        Integer depositAmount,
        @Schema(example = "0")
        Integer adjustmentAmount,
        @Schema(example = "15000")
        Long balanceAfter,
        @Schema(example = "2026-07-19")
        LocalDate date
){

}
