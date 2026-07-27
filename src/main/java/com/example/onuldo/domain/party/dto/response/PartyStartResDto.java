package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.enums.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyStartResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "ONGOING")
        PartyStatus status,
        @Schema(example = "2026-07-23T13:00:00")
        LocalDateTime startTriggeredAt
) {
    public static PartyStartResDto of(Party party) {
        return PartyStartResDto.builder()
                .partyId(party.getId())
                .status(party.getStatus())
                .startTriggeredAt(party.getStartTriggeredAt())
                .build();
    }
}
