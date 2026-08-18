package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record PartyWaitingResDto(
        Long partyId,
        String name,
        PartyStatus status,
        String inviteCode,
        Integer currentMembers,
        Integer maxMembers,
        Integer durationDays,
        Integer depositAmount,
        List<PartyMemberResDto> members,
        boolean isHost,
        boolean canStart
) {
    private static final int MIN_MEMBERS_TO_START = 2;

    // PAR-05: [시작하기]는 방장에게만 노출 — 2인 이상 + 파티원(방장 제외) 전원 준비완료 시 활성화
    public static PartyWaitingResDto of(Party party, List<PartyMember> partyMembers, Long requesterId) {
        List<PartyMemberResDto> memberResDtos = partyMembers.stream()
                .map(PartyMemberResDto::from)
                .toList();

        boolean isHost = party.getHostUser().getId().equals(requesterId);
        boolean allMembersReady = partyMembers.stream()
                .filter(member -> member.getRole() == PartyMemberRole.MEMBER)
                .allMatch(PartyMember::isReady);
        boolean canStart = isHost
                && partyMembers.size() >= MIN_MEMBERS_TO_START
                && allMembersReady;

        return PartyWaitingResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .status(party.getStatus())
                .inviteCode(party.getInviteCode())
                .currentMembers(partyMembers.size())
                .maxMembers(party.getMaxMembers())
                .durationDays(party.getDurationDays())
                .depositAmount(party.getDepositAmount())
                .members(memberResDtos)
                .isHost(isHost)
                .canStart(canStart)
                .build();
    }
}
