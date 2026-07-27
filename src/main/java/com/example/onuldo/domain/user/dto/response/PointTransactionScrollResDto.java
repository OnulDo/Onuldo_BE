package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PointTransactionScrollResDto (
        @Schema(description = "포인트 거래 내역")
        List<PointTransactionResDto> pointTransactions,
        @Schema(example = "2", nullable = true)
        Long nextCursor,
        @Schema(example = "true")
        boolean hasNext

){
}
