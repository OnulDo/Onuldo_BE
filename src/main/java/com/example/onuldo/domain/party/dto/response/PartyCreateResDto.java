package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyCreateResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "갓생팟")
        String name,
        @Schema(example = "82K3H9")
        String inviteCode,
        @Schema(example = "2026-08-20T00:00:00")
        LocalDateTime inviteExpiresAt,
        @Schema(example = "WAITING")
        PartyStatus status,
        @Schema(example = "5")
        Long hostUserId,
        @Schema(example = "4")
        Integer maxMembers,
        @Schema(example = "2026-07-23T13:00:00")
        LocalDateTime createdAt
) {
}
