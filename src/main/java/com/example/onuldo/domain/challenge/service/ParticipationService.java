package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public ParticipationResDto participatePersonalChallenge(
            Long userId,
            Long challengeId,
            ParticipationReqDto request
    ) {
        User user = userRepository.findByIdForUpdate(userId)
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

        long balanceAfter = user.getPointBalance() - request.depositAmount();
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        Participation participation = createPersonalParticipation(
                user, challenge, request.depositAmount(), request.durationWeeks(), startDate, endDate
        );
        participationRepository.save(participation);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.DEPOSIT)
                .amount(-request.depositAmount())
                .balanceAfter(balanceAfter)
                .description(challenge.getName())
                .build()
        );

        return ParticipationResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .durationWeeks(request.durationWeeks())
                .durationDays(durationDays)
                .depositAmount(request.depositAmount())
                .expectedRefundAmount(request.depositAmount())
                .build();
    }

    public UserChallengeListResDto getUserChallenges(Long userId, ParticipationStatus status) {
        List<Participation> participations = status == null
                ? participationRepository.findAllByUser_IdOrderByIdDesc(userId)
                : participationRepository.findAllByUser_IdAndStatusOrderByIdDesc(userId, status);

        return UserChallengeListResDto.builder()
                .challenges(participations.stream()
                        .map(this::toUserChallengeResDto)
                        .toList())
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
        Participation participation = Participation.builder()
                .user(user)
                .challenge(challenge)
                .party(null)
                .participationType(ParticipationType.PERSONAL)
                .depositAmount(depositAmount)
                .durationWeeks(durationWeeks)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        validateParticipationState(participation);
        return participation;
    }

    private void validateParticipationState(Participation participation) {
        if (participation.getParticipationType() == ParticipationType.PERSONAL && participation.getParty() != null) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "개인 참여에는 party가 연결되면 안 됩니다.");
        }

        if (participation.getParticipationType() == ParticipationType.PARTY && participation.getParty() == null) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "party 참여에는 party가 필요합니다.");
        }
    }

    private UserChallengeResDto toUserChallengeResDto(Participation participation) {
        Challenge challenge = participation.getChallenge();

        return UserChallengeResDto.builder()
                .participationId(participation.getId())
                .participationStatus(participation.getStatus())
                .participationType(participation.getParticipationType())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .challengeExplainContent(challenge.getExplainContent())
                .challengeDescription(challenge.getDescription())
                .challengeCaptionImgUrl(challenge.getCaptionImgUrl())
                .challengeVerifyMethodContent(challenge.getVerifyMethodContent())
                .challengeVerificationExamplePhotoUrl(challenge.getVerificationExamplePhotoUrl())
                .participantCount(challenge.getParticipantCount())
                .category(challenge.getCategory())
                .timeStart(challenge.getTimeStart())
                .timeEnd(challenge.getTimeEnd())
                .depositAmount(participation.getDepositAmount())
                .durationWeeks(participation.getDurationWeeks())
                .startDate(participation.getStartDate())
                .endDate(participation.getEndDate())
                .build();
    }
}
