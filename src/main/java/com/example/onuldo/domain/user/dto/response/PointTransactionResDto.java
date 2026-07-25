package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.PointTransactionType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PointTransactionResDto (
        PointTransactionType type,
        String title,
        Integer amount,
        Integer depositAmount,
        Integer adjustmentAmount,
        Long balanceAfter,
        LocalDate date
){

}
