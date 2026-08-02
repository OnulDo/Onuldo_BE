package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyMemberResDto(
        @Schema(example = "5")
        Long userId,
        @Schema(example = "김민지")
        String nickname,
        @Schema(example = "https://cdn.onuldo.com/profile/5.png", nullable = true)
        String profileImageUrl,
        @Schema(example = "HOST")
        PartyMemberRole role,
        @Schema(example = "WAITING")
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
