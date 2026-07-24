package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;

    public ParticipationResDto participatePersonalChallenge(Long userId, Long challengeId, ParticipationReqDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        validateDepositOption(challenge, request.depositAmount());
        validateAlreadyParticipating(userId, challengeId);
        validatePointBalance(user, request.depositAmount());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(request.durationWeeks());
        Integer durationDays = request.durationWeeks() * 7;

        user.setPointBalance(user.getPointBalance() - request.depositAmount());
        userRepository.save(user);

        Participation participation = createPersonalParticipation(
                user, challenge, request.depositAmount(), request.durationWeeks(), startDate, endDate
        );
        participationRepository.save(participation);

        return ParticipationResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .durationWeeks(request.durationWeeks())
                .durationDays(durationDays)
                .depositAmount(request.depositAmount())
                .expectedRefundAmount(request.depositAmount())
                .build();
    }

    private void validateDepositOption(Challenge challenge, Integer depositAmount) {
        if (challenge.getDepositOptionList() == null || !challenge.getDepositOptionList().contains(depositAmount)) {
            throw new RestApiException(GlobalErrorStatus._INVALID_DEPOSIT_OPTION);
        }
    }

    private void validateAlreadyParticipating(Long userId, Long challengeId) {
        if (participationRepository.existsByUser_IdAndChallenge_Id(userId, challengeId)) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_PARTICIPATING_CHALLENGE);
        }
    }

    private void validatePointBalance(User user, Integer depositAmount) {
        long shortage = depositAmount.longValue() - user.getPointBalance();
        if (shortage > 0) {
            throw new RestApiException(
                    GlobalErrorStatus._INSUFFICIENT_POINT_FOR_CHALLENGE,
                    "보유 포인트가 " + shortage + "P 부족합니다."
            );
        }
    }

    private Participation createPersonalParticipation(
            User user,
            Challenge challenge,
            Integer depositAmount,
            Integer durationWeeks,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Participation.builder()
                .user(user)
                .challenge(challenge)
                .party(null)
                .participationType(ParticipationType.PERSONAL)
                .depositAmount(depositAmount)
                .durationWeeks(durationWeeks)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
