package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordSummaryResDto;
import com.example.onuldo.domain.challenge.dto.response.OngoingChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationService {

    private static final int DAYS_PER_WEEK = 7;
    private static final int PERCENT_MULTIPLIER = 100;
    private static final List<ParticipationStatus> COMPLETED_STATUSES = List.of(
            ParticipationStatus.SUCCESS,
            ParticipationStatus.FAIL
    );

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TimeService timeService;

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

        LocalDate startDate = timeService.todayKst();
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

    public CursorPageResponse<UserChallengeResDto> getUserChallenges(
            Long userId,
            ParticipationStatus status,
            String cursor,
            int size
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);

        Long lastId = CursorKeyCodec.isBlank(cursor) ? null : CursorKeyCodec.decodeAsLongs(cursor, 1)[0];

        List<Participation> participations = status == null
                ? participationRepository.findAllByUser_IdOrderByIdDesc(userId, lastId, CursorPageable.of(resolvedSize))
                : participationRepository.findAllByUser_IdAndStatusOrderByIdDesc(userId, status, lastId, CursorPageable.of(resolvedSize));

        return CursorPageResponse.of(
                participations,
                resolvedSize,
                this::toUserChallengeResDto,
                p -> CursorKeyCodec.encode(p.getId())
        );

    }

    public DailyChallengeListResDto getDailyChallenges(Long userId, LocalDate date) {
        List<Participation> participations = participationRepository
                .findAllByUser_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
                        userId,
                        ParticipationStatus.ONGOING,
                        date,
                        date
                );

        Set<Long> verifiedChallengeIds = new HashSet<>(verificationRepository
                .findVerifiedChallengeIdsByUserIdAndVerificationDate(userId, date));

        return DailyChallengeListResDto.builder()
                .challenges(participations.stream()
                        .map(participation -> toDailyChallengeResDto(
                                participation,
                                verifiedChallengeIds.contains(participation.getChallenge().getId())
                        ))
                        .toList())
                .build();
    }

    public List<OngoingChallengeRecordResDto> getOngoingChallengeRecords(Long userId, LocalDate date) {
        List<Participation> ongoingParticipations =
                participationRepository.findAllWithChallengeByUserIdAndStatusOrderByIdDesc(
                        userId,
                        ParticipationStatus.ONGOING
                );

        Map<Long, Integer> achievementRateByParticipationId = calculateAchievementRates(ongoingParticipations);
        Set<Long> verifiedParticipationIds = getVerifiedParticipationIds(ongoingParticipations, date);

        return ongoingParticipations.stream()
                .map(participation -> toOngoingChallengeRecordResDto(
                        participation,
                        achievementRateByParticipationId.getOrDefault(participation.getId(), 0),
                        date,
                        verifiedParticipationIds
                ))
                .toList();
    }

    public CompletedChallengeRecordSummaryResDto getCompletedChallengeRecords(Long userId) {
        List<Participation> completedParticipations =
                participationRepository.findAllWithChallengeByUserIdAndStatusInOrderByEndDateDesc(
                        userId,
                        COMPLETED_STATUSES
                );

        Map<Long, Integer> achievementRateByParticipationId = calculateAchievementRates(completedParticipations);
        Map<Long, Integer> refundAmountByParticipationId = getRefundAmountByParticipationId(completedParticipations);

        List<CompletedChallengeRecordResDto> completedChallengeRecords = completedParticipations.stream()
                .map(participation -> toCompletedChallengeRecordResDto(
                        participation,
                        achievementRateByParticipationId.getOrDefault(participation.getId(), 0),
                        refundAmountByParticipationId.getOrDefault(participation.getId(), 0)
                ))
                .toList();

        return CompletedChallengeRecordSummaryResDto.builder()
                .totalCompletedCount(completedChallengeRecords.size())
                .successRate(calculateSuccessRate(completedParticipations))
                .totalSavedAmount(calculateTotalSavedAmount(refundAmountByParticipationId))
                .completedChallenges(completedChallengeRecords)
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

    private OngoingChallengeRecordResDto toOngoingChallengeRecordResDto(
            Participation participation,
            Integer achievementRate,
            LocalDate date,
            Set<Long> verifiedParticipationIds
    ) {
        Challenge challenge = participation.getChallenge();
        boolean isVerifiedToday = verifiedParticipationIds.contains(participation.getId());

        return OngoingChallengeRecordResDto.builder()
                .participationId(participation.getId())
                .challengeId(challenge.getId())
                .challengeTitle(challenge.getName())
                .isVerifiedToday(isVerifiedToday)
                .daysUntilEnd(calculateDaysUntilEnd(date, participation.getEndDate()))
                .achievementRate(achievementRate)
                .depositAmount(participation.getDepositAmount())
                .type(participation.getParticipationType())
                .build();
    }

    private CompletedChallengeRecordResDto toCompletedChallengeRecordResDto(
            Participation participation,
            Integer achievementRate,
            Integer refundAmount
    ) {
        Challenge challenge = participation.getChallenge();

        return CompletedChallengeRecordResDto.builder()
                .participationId(participation.getId())
                .challengeId(challenge.getId())
                .challengeTitle(challenge.getName())
                .resultStatus(participation.getStatus())
                .refundAmount(refundAmount)
                .endedDate(participation.getEndDate())
                .achievementRate(achievementRate)
                .build();
    }

    private Map<Long, Integer> calculateAchievementRates(List<Participation> participations) {
        if (participations.isEmpty()) {
            return Map.of();
        }

        Map<Long, Participation> participationById = participations.stream()
                .collect(Collectors.toMap(Participation::getId, Function.identity()));
        Map<Long, Map<LocalDate, BigDecimal>> dayScoreByParticipationId = new HashMap<>();

        verificationRepository.findAllByParticipation_IdIn(participationById.keySet())
                .forEach(verification -> {
                    Long participationId = verification.getParticipation().getId();
                    BigDecimal dayScore = resolveDayScore(verification);
                    dayScoreByParticipationId
                            .computeIfAbsent(participationId, ignored -> new HashMap<>())
                            .merge(verification.getVerificationDate(), dayScore, BigDecimal::max);
                });

        return participationById.values().stream()
                .collect(Collectors.toMap(
                        Participation::getId,
                        participation -> calculateAchievementRate(
                                participation,
                                dayScoreByParticipationId.getOrDefault(participation.getId(), Map.of())
                        )
                ));
    }

    private int calculateAchievementRate(
            Participation participation,
            Map<LocalDate, BigDecimal> dayScoreByDate
    ) {
        int durationDays = participation.getDurationWeeks() * DAYS_PER_WEEK;
        if (durationDays <= 0 || dayScoreByDate.isEmpty()) {
            return 0;
        }

        BigDecimal totalDayScore = dayScoreByDate.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int achievementRate = totalDayScore
                .divide(BigDecimal.valueOf(durationDays), 0, RoundingMode.HALF_UP)
                .intValue();

        return Math.min(PERCENT_MULTIPLIER, Math.max(0, achievementRate));
    }

    private BigDecimal resolveDayScore(Verification verification) {
        if (verification.getDayScore() != null) {
            return verification.getDayScore();
        }

        if (verification.getReview() == VerificationReviewStatus.AUTO_PASS) {
            return BigDecimal.valueOf(PERCENT_MULTIPLIER);
        }

        return BigDecimal.ZERO;
    }

    private Map<Long, Integer> getRefundAmountByParticipationId(List<Participation> completedParticipations) {
        if (completedParticipations.isEmpty()) {
            return Map.of();
        }

        List<Long> participationIds = completedParticipations.stream()
                .map(Participation::getId)
                .toList();

        return settlementRepository.findAllByParticipation_IdInOrderByIdDesc(participationIds).stream()
                .collect(Collectors.toMap(
                        settlement -> settlement.getParticipation().getId(),
                        // 정렬을 id DESC로 고정했으므로 같은 참여의 정산이 여러 건이면
                        // 마지막 원소가 가장 최근 정산이며, 그 값을 최종 환급액으로 사용한다.
                        Settlement::getRefundAmount,
                        (first, second) -> second
                ));
    }

    private long calculateTotalSavedAmount(Map<Long, Integer> refundAmountByParticipationId) {
        return refundAmountByParticipationId.values().stream()
                .mapToLong(Integer::longValue)
                .sum();
    }

    private int calculateSuccessRate(List<Participation> completedParticipations) {
        if (completedParticipations.isEmpty()) {
            return 0;
        }

        long successCount = completedParticipations.stream()
                .filter(participation -> participation.getStatus() == ParticipationStatus.SUCCESS)
                .count();

        return BigDecimal.valueOf(successCount)
                .multiply(BigDecimal.valueOf(PERCENT_MULTIPLIER))
                .divide(BigDecimal.valueOf(completedParticipations.size()), 0, RoundingMode.DOWN)
                .intValue();
    }

    private int calculateDaysUntilEnd(LocalDate date, LocalDate endDate) {
        long remainingDays = ChronoUnit.DAYS.between(date, endDate);
        return Math.toIntExact(Math.max(0, remainingDays));
    }

    private Set<Long> getVerifiedParticipationIds(List<Participation> participations, LocalDate date) {
        if (participations.isEmpty()) {
            return Set.of();
        }

        Set<Long> participationIds = participations.stream()
                .map(Participation::getId)
                .collect(Collectors.toSet());

        return verificationRepository.findAllByParticipation_IdInAndVerificationDate(participationIds, date).stream()
                .map(verification -> verification.getParticipation().getId())
                .collect(Collectors.toSet());
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

    private DailyChallengeResDto toDailyChallengeResDto(Participation participation, boolean verifiedOnDate) {
        Challenge challenge = participation.getChallenge();

        return DailyChallengeResDto.builder()
                .participationId(participation.getId())
                .participationStatus(participation.getStatus())
                .participationType(participation.getParticipationType())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .challengeExplainContent(challenge.getExplainContent())
                .challengeCaptionImgUrl(challenge.getCaptionImgUrl())
                .challengeVerifyMethodContent(challenge.getVerifyMethodContent())
                .participantCount(challenge.getParticipantCount())
                .category(challenge.getCategory())
                .timeStart(challenge.getTimeStart())
                .timeEnd(challenge.getTimeEnd())
                .depositAmount(participation.getDepositAmount())
                .durationWeeks(participation.getDurationWeeks())
                .startDate(participation.getStartDate())
                .endDate(participation.getEndDate())
                .verifiedOnDate(verifiedOnDate)
                .build();
    }
}
