package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyCreateResDto(
        Long partyId,
        String name,
        String inviteCode,
        LocalDateTime inviteExpiresAt,
        PartyStatus status,
        Long hostUserId,
        Integer maxMembers,
        LocalDateTime createdAt
) {
}
