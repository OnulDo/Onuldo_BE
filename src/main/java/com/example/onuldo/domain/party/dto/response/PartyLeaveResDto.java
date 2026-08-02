package com.example.onuldo.domain.party.dto.response;

import lombok.Builder;

@Builder
public record PartyLeaveResDto(
        Long partyId,
        boolean dissolved,
        Long newHostUserId
) {
}
