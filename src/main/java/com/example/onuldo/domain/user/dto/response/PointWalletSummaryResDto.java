package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

@Builder
public record PointWalletSummaryResDto (
        Long balance,
        Long pendingPoints,
        Long totalDeposit,
        Long totalRefund,
        Long totalPenalty,
        Integer averageReturnRate
){
}
