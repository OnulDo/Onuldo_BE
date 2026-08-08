package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.SettlementStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
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

@Service
@RequiredArgsConstructor
public class SettlementService {
    private static final BigDecimal SUCCESS_THRESHOLD = BigDecimal.valueOf(0.85);
    private static final BigDecimal FAILURE_REFUND_BASE = SUCCESS_THRESHOLD;
    private static final BigDecimal FAILURE_REFUND_EXPONENT = BigDecimal.ONE;
    private static final BigDecimal BONUS_RATE = BigDecimal.valueOf(0.025);

    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;

    /** 챌린지 정산 처리. <br>
     * 정산 호출 로직이 두개로 분산되어 (챌린지 성공 시, 인증 실패 챌린지 조회 스케줄러)<br>
     * 내부에 날짜에 따른 검증 로직을 구현하지 않음. <br><br>
     *
     * 따라서 호출 시 바로 정산 처리 됨으로 주의하여 호출
     * */
    @Transactional
    public void settleParticipatedChallenge(Long participationId) {
        Participation lockedParticipation = participationRepository.findByIdForUpdate(participationId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTICIPATION_NOT_FOUND));

        // 락 획득 후 다시 확인해서 동시성 중복 정산을 방지한다.
        if (settlementRepository.existsByParticipation_Id(participationId)) {
            return;
        }

        long totalDays = lockedParticipation.getDurationWeeks() * 7L;
        long passDays = verificationRepository.countByParticipation_IdAndReview(
                lockedParticipation.getId(),
                VerificationReviewStatus.PASS
        );

        // 면제 한도
        long exemptionDays = totalDays / 7;

        // rValue = min(1, (PASS 수 + 면제 한도) / totalDays
        BigDecimal rValue = BigDecimal.valueOf(passDays + exemptionDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        boolean success = rValue.compareTo(SUCCESS_THRESHOLD) >= 0;

        lockedParticipation.changeStatus(success ? ParticipationStatus.SUCCESS : ParticipationStatus.FAIL);

        if (success) {
            settleSuccess(lockedParticipation, rValue);
        } else {
            settleFailure(lockedParticipation, rValue);
        }
    }

    private void settleSuccess(Participation participation, BigDecimal rValue) {
        int bonusAmount = calculateBonusAmount(participation.getDepositAmount());
        int refundAmount = participation.getDepositAmount() + bonusAmount;

        refundPoint(participation, refundAmount, bonusAmount);

        Settlement resultSettlement = createSettlement(participation, rValue, refundAmount, bonusAmount);
        settlementRepository.save(resultSettlement);
        challengeNotificationService.notifySettlementCompleted(participation, refundAmount);
    }

    private void settleFailure(Participation participation, BigDecimal rValue) {
        int refundAmount = calculateFailureRefundAmount(participation.getDepositAmount(), rValue);
        int penaltyAmount = participation.getDepositAmount() - refundAmount;

        refundPoint(participation, refundAmount, -penaltyAmount);

        Settlement resultSettlement = createSettlement(participation, rValue, refundAmount, 0);

        settlementRepository.save(resultSettlement);
        challengeNotificationService.notifySettlementCompleted(participation, refundAmount);
    }

    private Settlement createSettlement(
            Participation participation,
            BigDecimal rValue,
            int refundAmount,
            int bonusAmount
    ) {
        return Settlement.builder()
                .participation(participation)
                .depositAmount(participation.getDepositAmount())
                .rValue(rValue)
                .refundAmount(refundAmount)
                .bonusAmount(bonusAmount)
                .partyShareAmount(0)
                .status(SettlementStatus.COMPLETED)
                .processedAt(timeService.nowKst())
                .build();
    }

    private void refundPoint(
            Participation participation,
            int refundAmount,
            int adjustmentAmount
    ) {
        User user = userRepository.findByIdForUpdate(participation.getUser().getId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        long balanceAfter = user.getPointBalance() + refundAmount;

        user.setPointBalance(balanceAfter);

        pointTransactionRepository.save(
            PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.REFUND)
                .amount(refundAmount)
                .depositAmount(participation.getDepositAmount())
                .adjustmentAmount(adjustmentAmount)
                .balanceAfter(balanceAfter)
                .description(participation.getChallenge().getName())
                .refType("SETTLEMENT")
                .refId(participation.getId())
                .build()
        );
    }

    private int calculateBonusAmount(int depositAmount) {
        return BigDecimal.valueOf(depositAmount)
                .multiply(BONUS_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int calculateFailureRefundAmount(
            int depositAmount,
            BigDecimal rValue
    ) {
        BigDecimal normalizedRate = rValue.divide(
                FAILURE_REFUND_BASE,
                4,
                RoundingMode.HALF_UP
        );

        BigDecimal poweredRate = normalizedRate.pow(
                FAILURE_REFUND_EXPONENT.intValueExact()
        );

        return BigDecimal.valueOf(depositAmount)
                .multiply(poweredRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
