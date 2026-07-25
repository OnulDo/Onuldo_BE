package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

@Builder
public record ChargePointResDto (
    Integer amount,
    Long balanceAfter
){

}
