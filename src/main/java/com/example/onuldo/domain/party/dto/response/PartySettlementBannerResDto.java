package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartySettlementBannerResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "새벽 러너 파티")
        String partyName
) {
}
