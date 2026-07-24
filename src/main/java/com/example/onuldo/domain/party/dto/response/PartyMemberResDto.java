package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyMemberStatus;
import lombok.Builder;

@Builder
public record PartyMemberResDto(
        Long userId,
        String nickname,
        String profileImageUrl,
        PartyMemberRole role,
        PartyMemberStatus status
) {
    public static PartyMemberResDto from(PartyMember partyMember) {
        return PartyMemberResDto.builder()
                .userId(partyMember.getUser().getId())
                .nickname(partyMember.getUser().getNickname())
                .profileImageUrl(partyMember.getUser().getProfileImageUrl())
                .role(partyMember.getRole())
                .status(partyMember.getStatus())
                .build();
    }
}
