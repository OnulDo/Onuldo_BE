package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.SettlementStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POI-07/08 파티 정산
 */
@Service
@RequiredArgsConstructor
public class PartySettlementService {

    // 일별 귀속을 소수점 정확값으로 유지하기 위한 내부 계산 스케일
    private static final int CALC_SCALE = 10;
    // 파티 참여 상태 라벨링 기준 (개인과 동일한 85% 재사용, 금액 계산과 무관)
    private static final BigDecimal SUCCESS_THRESHOLD = BigDecimal.valueOf(0.85);
    // POI-07 전원 완주 보너스 b = 5% (개인 성공 보너스 2.5%와 다름)
    private static final BigDecimal BONUS_RATE = BigDecimal.valueOf(0.05);
    // POI-08 직접검토 유예 시간
    private static final long MANUAL_REVIEW_GRACE_HOURS = 24;

    private final PartyRepository partyRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TimeService timeService;

    /**
     * 파티 단위 1회 정산. 마지막 수행일 인증 마감 후에만 처리
     * 파티 락으로 동시 트리거(파티원별 인증 완료 + 파티 정산 스케줄러)를 직렬화
     */
    @Transactional
    public void settleParty(Long partyId) {
        partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        // POI-08: 이미 정산된 파티면 재정산하지 않는다 (중복 정산·이중 지급 방어).
        if (settlementRepository.existsByParticipation_Party_Id(partyId)) {
            return;
        }

        List<Participation> participations =
                participationRepository.findAllByParty_IdAndStatus(partyId, ParticipationStatus.ONGOING);
        if (participations.isEmpty()) {
            return;
        }

        // 전원 마지막 일 인증 마감 전이거나, 직접검토 중인 인증의 유예 기간이 안 지났으면 정산 보류
        if (!isSettlementWindowClosed(participations)) {
            return;
        }

        processSettlement(participations);
    }

    private boolean isSettlementWindowClosed(List<Participation> participations) {
        Participation reference = participations.get(0);
        LocalDate endDate = reference.getEndDate();
        LocalTime deadline = reference.getChallenge().getTimeEnd();
        LocalDateTime now = timeService.nowKst();

        boolean timeWindowClosed;
        if (now.toLocalDate().isAfter(endDate)) {
            timeWindowClosed = true;
        } else if (now.toLocalDate().isEqual(endDate)) {
            timeWindowClosed = deadline == null || !now.toLocalTime().isBefore(deadline);
        } else {
            timeWindowClosed = false;
        }
        if (!timeWindowClosed) {
            return false;
        }

        // POI-08: 직접검토(MANUAL_REVIEW) 중인 인증이 있으면 검토 확정까지 최대 24시간 정산을 미룬다.
        // 검토가 PASS로 확정되면 collectPassDates가 review=PASS만 카운트하므로 별도 처리 없이 그날이 자동으로 성공 반영된다.
        List<Long> participationIds = participations.stream().map(Participation::getId).toList();
        LocalDateTime manualReviewGraceCutoff = now.minusHours(MANUAL_REVIEW_GRACE_HOURS);
        boolean hasPendingManualReview = verificationRepository.existsByParticipation_IdInAndReviewAndVerifiedAtAfter(
                participationIds, VerificationReviewStatus.MANUAL_REVIEW, manualReviewGraceCutoff);
        return !hasPendingManualReview;
    }

    private void processSettlement(List<Participation> participations) {
        Participation reference = participations.get(0);
        int depositAmount = reference.getDepositAmount();
        LocalDate startDate = reference.getStartDate();
        LocalDate endDate = reference.getEndDate();
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate);
        int memberCount = participations.size();

        // 수행일이 0 이하면(시작일=종료일 등) 일 지분 나눗셈이 불가능하고, 재시도해도 데이터가 바뀌지 않아
        // 스케줄러가 매번 같은 예외를 반복하며 영구 미정산 상태로 남는다 — 정산 대상에서 명시적으로 제외한다.
        if (totalDays <= 0) {
            throw new RestApiException(GlobalErrorStatus._SETTLEMENT_INVALID_PERIOD);
        }

        // 수행일 집합: 시작일(예치일) 다음 날 ~ 종료일 (개인 모델과 동일하게 종료일이 마지막 인증일)
        List<LocalDate> performanceDates = startDate.plusDays(1)
                .datesUntil(endDate.plusDays(1))
                .toList();
        Map<Long, Set<LocalDate>> passDates = collectPassDates(participations, startDate, endDate);

        // 일 지분 s = 도전금 ÷ 총 수행일 (선형 균등)
        BigDecimal dayShare = BigDecimal.valueOf(depositAmount)
                .divide(BigDecimal.valueOf(totalDays), CALC_SCALE, RoundingMode.HALF_UP);
        Map<Long, BigDecimal> accruedDistribution = distributeDaily(performanceDates, passDates, dayShare, memberCount);

        // 전원 완주 보너스: 전원이 전 수행일을 성공한 경우에만 지급
        boolean allComplete = passDates.values().stream()
                .allMatch(dates -> dates.size() == totalDays);
        int bonusAmount = allComplete
                ? BigDecimal.valueOf(depositAmount).multiply(BONUS_RATE).setScale(0, RoundingMode.HALF_UP).intValue()
                : 0;

        for (Participation participation : participations) {
            settleMember(participation, passDates.get(participation.getId()),
                    accruedDistribution.get(participation.getId()), dayShare, totalDays, bonusAmount);
        }
    }

    /**
     * 매일 실패자의 당일 지분을 그날 성공자에게 1/n 균등 분배한 누적 귀속액(소수점 정확값)을 참여 ID별로 계산한다.
     * 전원 실패일은 운영자 회수(분배 없음), 전원 성공일은 몰수·분배 없음.
     */
    private Map<Long, BigDecimal> distributeDaily(
            List<LocalDate> performanceDates,
            Map<Long, Set<LocalDate>> passDates,
            BigDecimal dayShare,
            int memberCount
    ) {
        Map<Long, BigDecimal> accrued = new HashMap<>();
        for (Long participationId : passDates.keySet()) {
            accrued.put(participationId, BigDecimal.ZERO);
        }

        for (LocalDate date : performanceDates) {
            List<Long> successParticipations = passDates.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(date))
                    .map(Map.Entry::getKey)
                    .toList();

            int successCount = successParticipations.size();
            if (successCount == 0 || successCount == memberCount) {
                continue;
            }

            int failureCount = memberCount - successCount;
            BigDecimal sharePerSuccess = dayShare.multiply(BigDecimal.valueOf(failureCount))
                    .divide(BigDecimal.valueOf(successCount), CALC_SCALE, RoundingMode.HALF_UP);

            for (Long participationId : successParticipations) {
                accrued.merge(participationId, sharePerSuccess, BigDecimal::add);
            }
        }
        return accrued;
    }

    private Map<Long, Set<LocalDate>> collectPassDates(
            List<Participation> participations,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Long> participationIds = participations.stream()
                .map(Participation::getId)
                .toList();

        // 인증이 없는 파티원도 빈 집합으로 포함해야 총원 수가 맞다.
        Map<Long, Set<LocalDate>> passDates = new HashMap<>();
        for (Long participationId : participationIds) {
            passDates.put(participationId, new HashSet<>());
        }

        for (Verification verification : verificationRepository.findAllByParticipation_IdIn(participationIds)) {
            if (verification.getReview() != VerificationReviewStatus.PASS) {
                continue;
            }

            LocalDate verifiedDate = verification.getVerificationDate();
            // 수행일 범위(시작일 다음 날 ~ 종료일) 밖의 인증은 정산 대상에서 제외한다.
            if (verifiedDate.isAfter(startDate) && !verifiedDate.isAfter(endDate)) {
                passDates.get(verification.getParticipation().getId()).add(verifiedDate);
            }
        }
        return passDates;
    }

    private void settleMember(
            Participation participation,
            Set<LocalDate> memberPassDates,
            BigDecimal accruedDistribution,
            BigDecimal dayShare,
            int totalDays,
            int bonusAmount
    ) {
        int successDays = memberPassDates.size();
        // 잔여 예치금 = 성공일수 × s (몰수된 실패일 지분 제외)
        BigDecimal remainingDeposit = dayShare.multiply(BigDecimal.valueOf(successDays));

        // 지급 항목별로 1P 미만 올림(단수 차액은 운영 부담, 유저 유리 원칙)해서 화면에 "도전금 환급"/"분배금"을
        // 각각 정확한 금액으로 나눠 보여줄 수 있게 한다. 항목별로 올림한 값을 그대로 더하므로 총액과도 항상 일치한다.
        int depositRefundAmount = remainingDeposit.setScale(0, RoundingMode.CEILING).intValue();
        int partyShareAmount = accruedDistribution.setScale(0, RoundingMode.CEILING).intValue();
        int payoutAmount = depositRefundAmount + partyShareAmount + bonusAmount;

        BigDecimal achievementRate = BigDecimal.valueOf(successDays)
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
        ParticipationStatus status = achievementRate.compareTo(SUCCESS_THRESHOLD) >= 0
                ? ParticipationStatus.SUCCESS
                : ParticipationStatus.FAIL;

        participation.changeStatus(status);
        payoutPoint(participation, payoutAmount);

        settlementRepository.save(Settlement.builder()
                .participation(participation)
                .depositAmount(participation.getDepositAmount())
                .rValue(achievementRate)
                .refundAmount(payoutAmount)
                .depositRefundAmount(depositRefundAmount)
                .bonusAmount(bonusAmount)
                .partyShareAmount(partyShareAmount)
                .status(SettlementStatus.COMPLETED)
                .processedAt(timeService.nowKst())
                .build());
    }

    // 지갑 잔액 반영 + 원장 기록(REFUND, net) — 개인 환급과 동일한 방식으로 통일해 이중 지급 방어를 단순화
    private void payoutPoint(Participation participation, int payoutAmount) {
        User user = userRepository.findByIdForUpdate(participation.getUser().getId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        long balanceAfter = user.getPointBalance() + payoutAmount;
        user.setPointBalance(balanceAfter);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.REFUND)
                .amount(payoutAmount)
                .depositAmount(participation.getDepositAmount())
                .adjustmentAmount(payoutAmount - participation.getDepositAmount())
                .balanceAfter(balanceAfter)
                .description(participation.getChallenge().getName())
                .refType("SETTLEMENT")
                .refId(participation.getId())
                .build());
    }
}
