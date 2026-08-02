package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PartyFeedResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "갓생팟")
        String name,
        @Schema(example = "30일 헬스")
        String challengeTitle,
        @Schema(example = "0.72")
        Double progressRate,
        @Schema(example = "3")
        Integer verifiedMemberCount,
        @Schema(example = "4")
        Integer totalMemberCount,
        @Schema(description = "파티원 진행 현황")
        List<PartyFeedItemResDto> members
) {
}
