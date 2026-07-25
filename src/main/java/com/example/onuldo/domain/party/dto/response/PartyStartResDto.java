package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyStartResDto(
        Long partyId,
        PartyStatus status,
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
