package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyLeaveResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "false")
        boolean dissolved,
        @Schema(example = "102")
        Long newHostUserId
) {
}
