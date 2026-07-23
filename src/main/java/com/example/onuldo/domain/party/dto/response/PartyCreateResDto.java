package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.Party;
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
    // Converter 클래스 없이 정적 팩토리로 Entity -> DTO 변환
    public static PartyCreateResDto from(Party party) {
        return PartyCreateResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .inviteCode(party.getInviteCode())
                .inviteExpiresAt(party.getInviteExpiresAt())
                .status(party.getStatus())
                .hostUserId(party.getHostUser().getId())
                .maxMembers(party.getMaxMembers())
                .createdAt(party.getCreatedAt())
                .build();
    }
}
