package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PointTransactionScrollResDto (
        List<PointTransactionResDto> pointTransactions,
        Long nextCursor,
        boolean hasNext

){
}
