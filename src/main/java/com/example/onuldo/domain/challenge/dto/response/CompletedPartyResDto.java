package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CompletedPartyResDto (
        @Schema(example = "5")
        Long partyId,
        @Schema(example = "새벽 러너 파티")
        String partyName,
        @Schema(example = "2")
        Long challengeId,
        @Schema(example = "2026-07-31T07:05:00")
        LocalDateTime verifiedAt,
        @Schema(example = "3")
        Integer totalMemberCount,
        @Schema(example = "3")
        Integer verifiedMemberCount
){
}
