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
        @Schema(description = "도전금 환급분 (보너스·파티 분배금 제외)", example = "30000")
        Integer myDepositRefundAmount,
        @Schema(description = "성과에 따른 보너스·파티 분배금(양수) 또는 차감액(음수)", example = "10000")
        Integer myDisplayAmount,
        List<PartyMemberResultResDto> members
) {
}
