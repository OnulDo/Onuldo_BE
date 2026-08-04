package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartySettlementResultType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PartyResultResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "정산 데모 파티")
        String name,
        @Schema(example = "PARTIAL_SUCCESS")
        PartySettlementResultType resultType,
        @Schema(example = "30000")
        Integer myRefundAmount,
        @Schema(example = "10000")
        Integer myDisplayAmount,
        List<PartyMemberResultResDto> members
) {
}
