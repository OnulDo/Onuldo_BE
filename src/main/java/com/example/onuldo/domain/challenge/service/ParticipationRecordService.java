package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordSummaryResDto;
import com.example.onuldo.domain.challenge.dto.response.OngoingChallengeRecordResDto;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationRecordService {

    private static final int DAYS_PER_WEEK = 7;
    private static final int PERCENT_MULTIPLIER = 100;
    private static final List<ParticipationStatus> COMPLETED_STATUSES = List.of(
            ParticipationStatus.SUCCESS,
            ParticipationStatus.FAIL
    );

    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;

    public List<OngoingChallengeRecordResDto> getOngoingChallengeRecords(Long userId, LocalDate date) {
        List<Participation> ongoingParticipations =
                participationRepository.findAllWithChallengeByUserIdAndStatusOrderByIdDesc(
                        userId,
                        ParticipationStatus.ONGOING
                );

        Map<Long, Integer> achievementRateByParticipationId = calculateOngoingAchievementRates(ongoingParticipations, date);
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

        Map<Long, Integer> achievementRateByParticipationId = calculateCompletedAchievementRates(completedParticipations);
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

    private OngoingChallengeRecordResDto toOngoingChallengeRecordResDto(
            Participation participation,
            Integer achievementRate,
            LocalDate date,
            Set<Long> verifiedParticipationIds
    ) {
        var challenge = participation.getChallenge();
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
        var challenge = participation.getChallenge();

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

    private Map<Long, Integer> calculateOngoingAchievementRates(List<Participation> participations, LocalDate date) {
        return calculateAchievementRates(
                participations,
                (participation, dayScoreByParticipationId) -> calculateOngoingAchievementRate(
                        participation,
                        date,
                        dayScoreByParticipationId
                )
        );
    }

    private Map<Long, Integer> calculateCompletedAchievementRates(List<Participation> participations) {
        return calculateAchievementRates(
                participations,
                this::calculateCompletedAchievementRate
        );
    }

    private int calculateOngoingAchievementRate(
            Participation participation,
            LocalDate date,
            Map<LocalDate, BigDecimal> dayScoreByDate
    ) {
        int elapsedDays = calculateInclusiveDays(participation.getStartDate(), date);
        if (elapsedDays <= 0 || dayScoreByDate.isEmpty()) {
            return 0;
        }

        BigDecimal totalDayScore = dayScoreByDate.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int achievementRate = totalDayScore
                .divide(BigDecimal.valueOf(elapsedDays), 0, RoundingMode.HALF_UP)
                .intValue();

        return Math.min(PERCENT_MULTIPLIER, Math.max(0, achievementRate));
    }

    private int calculateCompletedAchievementRate(
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

    private Map<Long, Integer> calculateAchievementRates(
            List<Participation> participations,
            BiFunction<Participation, Map<LocalDate, BigDecimal>, Integer> calculator
    ) {
        if (participations.isEmpty()) {
            return Map.of();
        }

        Map<Long, Participation> participationById = participations.stream()
                .collect(Collectors.toMap(Participation::getId, Function.identity()));
        Map<Long, Map<LocalDate, BigDecimal>> dayScoreByParticipationId = collectDayScores(participationById.keySet());

        return participationById.values().stream()
                .collect(Collectors.toMap(
                        Participation::getId,
                        participation -> calculator.apply(
                                participation,
                                dayScoreByParticipationId.getOrDefault(participation.getId(), Map.of())
                        )
                ));
    }

    private Map<Long, Map<LocalDate, BigDecimal>> collectDayScores(Set<Long> participationIds) {
        Map<Long, Map<LocalDate, BigDecimal>> dayScoreByParticipationId = new HashMap<>();

        verificationRepository.findAllByParticipation_IdIn(participationIds)
                .forEach(verification -> {
                    Long participationId = verification.getParticipation().getId();
                    BigDecimal dayScore = resolveDayScore(verification);
                    dayScoreByParticipationId
                            .computeIfAbsent(participationId, ignored -> new HashMap<>())
                            .merge(verification.getVerificationDate(), dayScore, BigDecimal::max);
                });

        return dayScoreByParticipationId;
    }

    private BigDecimal resolveDayScore(Verification verification) {
        if (verification.getDayScore() != null) {
            return verification.getDayScore();
        }

        if (verification.getReview() == VerificationReviewStatus.PASS) {
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

    private int calculateInclusiveDays(LocalDate startDate, LocalDate endDate) {
        long elapsedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return Math.toIntExact(Math.max(1, elapsedDays));
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
}
