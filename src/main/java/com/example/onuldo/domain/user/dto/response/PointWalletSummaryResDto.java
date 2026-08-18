package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PointWalletSummaryResDto (
        @Schema(example = "10000")
        Long balance,
        @Schema(example = "0")
        Long pendingPoints,
        @Schema(example = "5000")
        Long totalDeposit,
        @Schema(example = "5000")
        Long totalRefund,
        @Schema(example = "500")
        Long totalPenalty,
        @Schema(example = "100")
        Integer averageReturnRate
){
}
