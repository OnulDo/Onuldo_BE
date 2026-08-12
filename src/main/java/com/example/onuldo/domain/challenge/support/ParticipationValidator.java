package com.example.onuldo.domain.challenge.support;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.party.repository.PartyMemberRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.DuplicateException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ParticipationValidator {

    // 파티가 시작(ONGOING)되면 Participation이 생겨 아래 ONGOING 체크로 잡히므로,
    // 여기서는 아직 Participation이 없는 WAITING 소속만 별도로 확인한다.
    private static final List<PartyStatus> WAITING_PARTY_STATUSES = List.of(PartyStatus.WAITING);

    private final ParticipationRepository participationRepository;
    private final PartyMemberRepository partyMemberRepository;

    // 개인/파티 참여 구분 없이, 같은 챌린지를 이미 진행 중이거나 그 챌린지 파티에서 대기 중이면
    // 새 파티/챌린지 생성·참여를 막는다.
    public void validateNotOngoing(Long userId, Long challengeId) {
        boolean hasWaitingPartyMembership = partyMemberRepository
                .existsActiveMembershipByUser_IdAndChallenge_Id(userId, challengeId, WAITING_PARTY_STATUSES);
        if (hasWaitingPartyMembership) {
            throw new DuplicateException(ErrorStatus._CHALLENGE_PARTY_ALREADY_WAITING);
        }

        boolean hasOngoingParticipation = participationRepository
                .existsByUser_IdAndChallenge_IdAndStatus(userId, challengeId, ParticipationStatus.ONGOING);
        if (hasOngoingParticipation) {
            throw new DuplicateException(ErrorStatus._ALREADY_PARTICIPATING_CHALLENGE);
        }
    }
}
