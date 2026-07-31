package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartySettlementResultType;
import lombok.Builder;

import java.util.List;

@Builder
public record PartyResultResDto(
        Long partyId,
        String name,
        PartySettlementResultType resultType,
        Integer myRefundAmount,
        Integer myDisplayAmount,
        List<PartyMemberResultResDto> members
) {
}
