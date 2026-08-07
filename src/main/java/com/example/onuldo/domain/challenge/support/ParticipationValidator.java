package com.example.onuldo.domain.challenge.support;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipationValidator {

    private final ParticipationRepository participationRepository;

    // 개인/파티 참여 구분 없이, 같은 챌린지를 이미 진행 중이면 새 파티/챌린지 생성·참여를 막는다.
    public void validateNotOngoing(Long userId, Long challengeId) {
        if (participationRepository.existsByUser_IdAndChallenge_IdAndStatus(
                userId, challengeId, ParticipationStatus.ONGOING)) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_PARTICIPATING_CHALLENGE);
        }
    }
}
