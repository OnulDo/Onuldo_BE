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
import com.example.onuldo.global.common.time.TimeService;
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
    private final TimeService timeService;

    public List<OngoingChallengeRecordResDto> getOngoingChallengeRecords(Long userId) {
        LocalDate date = timeService.todayKst();

        List<Participation> ongoingParticipations =
                participationRepository.findAllWithChallengeByUserIdAndStatusOrderByIdDesc(
                        userId,
                        ParticipationStatus.ONGOING
                );

        Map<Long, Set<LocalDate>> verifiedDatesByParticipationId = getVerifiedDatesByParticipationId(ongoingParticipations, date);
        Set<Long> verifiedParticipationIds = getVerifiedParticipationIds(ongoingParticipations, date);

        return ongoingParticipations.stream()
                .map(participation -> toOngoingChallengeRecordResDto(
                        participation,
                        calculateOngoingAchievementRate(participation, date, verifiedDatesByParticipationId),
                        date,
                        verifiedParticipationIds.contains(participation.getId())
                )
            ).toList();
    }

    public CompletedChallengeRecordSummaryResDto getCompletedChallengeRecords(Long userId) {
        List<Participation> completedParticipations =
                participationRepository.findAllWithChallengeByUserIdAndStatusInOrderByEndDateDesc(
                        userId,
                        COMPLETED_STATUSES
                );

        Map<Long, Map<LocalDate, BigDecimal>> dayScoreByParticipationId = collectDayScores(completedParticipations);
        Map<Long, Settlement> settlementByParticipationId = getSettlementByParticipationId(completedParticipations);

        List<CompletedChallengeRecordResDto> completedChallengeRecords = completedParticipations.stream()
                .map(participation -> toCompletedChallengeRecordResDto(
                        participation,
                        calculateCompletedAchievementRate(
                                participation,
                                dayScoreByParticipationId.getOrDefault(participation.getId(), Map.of())
                        ),
                        settlementByParticipationId.get(participation.getId())
                ))
                .toList();

        return CompletedChallengeRecordSummaryResDto.builder()
                .totalCompletedCount(completedChallengeRecords.size())
                .successRate(calculateSuccessRate(completedParticipations))
                .totalSavedAmount(calculateTotalSavedAmount(settlementByParticipationId))
                .completedChallenges(completedChallengeRecords)
                .build();
    }

    private OngoingChallengeRecordResDto toOngoingChallengeRecordResDto(
            Participation participation,
            Integer achievementRate,
            LocalDate date,
            Boolean isVerifiedToday
    ) {
        var challenge = participation.getChallenge();

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
            Settlement settlement
    ) {
        var challenge = participation.getChallenge();
        int depositAmount = settlement != null ? settlement.getDepositAmount() : 0;
        int adjustmentAmount = settlement != null ? settlement.getRefundAmount() - depositAmount : 0;

        return CompletedChallengeRecordResDto.builder()
                .participationId(participation.getId())
                .challengeId(challenge.getId())
                .challengeTitle(challenge.getName())
                .resultStatus(participation.getStatus())
                .depositAmount(depositAmount)
                .adjustmentAmount(adjustmentAmount)
                .endedDate(participation.getEndDate())
                .achievementRate(achievementRate)
                .build();
    }

    private int calculateOngoingAchievementRate(
            Participation participation,
            LocalDate date,
            Map<Long, Set<LocalDate>> verifiedDatesByParticipationId
    ) {
        int elapsedDays = calculateInclusiveDays(participation.getStartDate(), date);
        Set<LocalDate> verifiedDates = verifiedDatesByParticipationId.getOrDefault(participation.getId(), Set.of());
        if (elapsedDays <= 0 || verifiedDates.isEmpty()) {
            return 0;
        }

        return BigDecimal.valueOf(verifiedDates.size())
                .multiply(BigDecimal.valueOf(PERCENT_MULTIPLIER))
                .divide(BigDecimal.valueOf(elapsedDays), 0, RoundingMode.HALF_UP)
                .intValue();
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

    private Map<Long, Map<LocalDate, BigDecimal>> collectDayScores(List<Participation> participations) {
        Map<Long, Map<LocalDate, BigDecimal>> dayScoreByParticipationId = new HashMap<>();
        Set<Long> participationIds = participations.stream()
                .map(Participation::getId)
                .collect(Collectors.toSet());

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

    private Map<Long, Settlement> getSettlementByParticipationId(List<Participation> completedParticipations) {
        if (completedParticipations.isEmpty()) {
            return Map.of();
        }

        List<Long> participationIds = completedParticipations.stream()
                .map(Participation::getId)
                .toList();

        return settlementRepository.findAllByParticipation_IdInOrderByIdDesc(participationIds).stream()
                .collect(Collectors.toMap(
                        settlement -> settlement.getParticipation().getId(),
                        settlement -> settlement,
                        (first, second) -> first
                ));
    }

    private long calculateTotalSavedAmount(Map<Long, Settlement> settlementByParticipationId) {
        return settlementByParticipationId.values().stream()
                .mapToLong(Settlement::getRefundAmount)
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

    private Map<Long, Set<LocalDate>> getVerifiedDatesByParticipationId(List<Participation> participations, LocalDate date) {
        if (participations.isEmpty()) {
            return Map.of();
        }

        Set<Long> participationIds = participations.stream()
                .map(Participation::getId)
                .collect(Collectors.toSet());

        return verificationRepository.findAllByParticipation_IdIn(participationIds).stream()
                .filter(verification -> verification.getReview() == VerificationReviewStatus.PASS)
                .filter(verification -> !verification.getVerificationDate().isAfter(date))
                .collect(Collectors.groupingBy(
                        verification -> verification.getParticipation().getId(),
                        Collectors.mapping(Verification::getVerificationDate, Collectors.toSet())
                ));
    }

    private Set<Long> getVerifiedParticipationIds(List<Participation> participations, LocalDate date) {
        if (participations.isEmpty()) {
            return Set.of();
        }

        Set<Long> participationIds = participations.stream()
                .map(Participation::getId)
                .collect(Collectors.toSet());

        return verificationRepository.findAllByParticipation_IdInAndVerificationDate(participationIds, date).stream()
                .filter(verification -> verification.getReview() == VerificationReviewStatus.PASS)
                .map(verification -> verification.getParticipation().getId())
                .collect(Collectors.toSet());
    }
}
