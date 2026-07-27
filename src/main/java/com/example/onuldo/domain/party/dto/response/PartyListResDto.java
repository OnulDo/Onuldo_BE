package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record PartyListResDto(
        Long partyId,
        String name,
        String challengeTitle,
        PartyStatus status,
        LocalDate endDate,
        LocalTime verificationDeadline,
        Double progressRate,
        Integer verifiedMemberCount,
        Integer totalMemberCount
) {
}
