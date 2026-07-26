package com.example.onuldo.domain.party.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PartyFeedResDto(
        Long partyId,
        String name,
        String challengeTitle,
        Double progressRate,
        Integer verifiedMemberCount,
        Integer totalMemberCount,
        List<PartyFeedItemResDto> members
) {
}
