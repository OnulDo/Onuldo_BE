package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.ChallengePot;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.PotTransaction;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.enums.PotTransactionType;
import com.example.onuldo.domain.challenge.enums.SettlementStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ChallengePotRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.PotTransactionRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.domain.user.support.PointBalanceCalculator;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
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
    // Participation.appliedBonusRate가 null인 레거시 참가(이 기능 도입 전 생성)에 쓰는 폴백 β
    private static final BigDecimal LEGACY_BONUS_RATE_FALLBACK = BigDecimal.valueOf(0.025);
    // 개인 챌린지 몰수금 pot은 싱글톤 행 하나로 운용한다.
    private static final Long CHALLENGE_POT_ID = 1L;

    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final PartySettlementService partySettlementService;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengePotRepository challengePotRepository;
    private final PotTransactionRepository potTransactionRepository;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;

    /** 챌린지 정산 처리.
     * 정산 호출 로직이 두개로 분산되어 (챌린지 성공 시, 인증 실패 챌린지 조회 스케줄러)
     * 내부에 날짜에 따른 검증 로직을 구현하지 않음.
     *
     * 따라서 호출 시 바로 정산 처리 됨으로 주의하여 호출
     * */
    @Transactional
    public void settleParticipatedChallenge(Long participationId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTICIPATION_NOT_FOUND));

        // 파티 참여는 개인 공식이 아니라 파티 단위(POI-07/08)로 정산한다.
        if (participation.getParticipationType() == ParticipationType.PARTY) {
            partySettlementService.settleParty(participation.getParty().getId());
            return;
        }

        settlePersonalChallenge(participationId);
    }

    private void settlePersonalChallenge(Long participationId) {
        Participation lockedParticipation = participationRepository.findByIdForUpdate(participationId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTICIPATION_NOT_FOUND));

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
        BigDecimal bonusRate = participation.getAppliedBonusRate() != null
                ? participation.getAppliedBonusRate()
                : LEGACY_BONUS_RATE_FALLBACK;
        int bonusAmount = calculateBonusAmount(participation.getDepositAmount(), bonusRate);
        int depositRefundAmount = participation.getDepositAmount();
        int refundAmount = depositRefundAmount + bonusAmount;

        refundPoint(participation, refundAmount, bonusAmount);

        Settlement resultSettlement = createSettlement(participation, rValue, refundAmount, depositRefundAmount, bonusAmount);
        settlementRepository.save(resultSettlement);
        challengeNotificationService.notifySettlementCompleted(participation, refundAmount);

        payBonusFromPot(resultSettlement, bonusAmount);
    }

    private void settleFailure(Participation participation, BigDecimal rValue) {
        int refundAmount = calculateFailureRefundAmount(participation.getDepositAmount(), rValue);
        int penaltyAmount = participation.getDepositAmount() - refundAmount;

        refundPoint(participation, refundAmount, -penaltyAmount);

        // 개인 실패 정산엔 보너스·분배금 개념이 없어 전액이 곧 예치금 환급분이다.
        Settlement resultSettlement = createSettlement(participation, rValue, refundAmount, refundAmount, 0);
        settlementRepository.save(resultSettlement);
        challengeNotificationService.notifySettlementCompleted(participation, refundAmount);

        accumulateForfeitureToPot(resultSettlement, penaltyAmount);
    }

    // 개인 챌린지 실패자 몰수금(도전금 - 환급액)을 pot에 적립하고 원장(PotTransaction)에 남긴다.
    private void accumulateForfeitureToPot(Settlement settlement, int penaltyAmount) {
        if (penaltyAmount <= 0) {
            return;
        }

        ChallengePot pot = challengePotRepository.findByIdForUpdate(CHALLENGE_POT_ID)
                .orElseThrow(() -> new InternalServerException(ErrorStatus._CHALLENGE_POT_NOT_FOUND));
        pot.accumulateForfeiture(penaltyAmount);

        potTransactionRepository.save(
            PotTransaction.builder()
                .type(PotTransactionType.FORFEITURE)
                .amount(penaltyAmount)
                .balanceAfter(pot.getBalance())
                .settlement(settlement)
                .description(describePotTransaction(settlement))
                .build()
        );
    }

    // 개인 챌린지 성공자 보너스를 pot에서 차감(마이너스 허용)하고 원장(PotTransaction)에 남긴다.
    private void payBonusFromPot(Settlement settlement, int bonusAmount) {
        if (bonusAmount <= 0) {
            return;
        }

        ChallengePot pot = challengePotRepository.findByIdForUpdate(CHALLENGE_POT_ID)
                .orElseThrow(() -> new InternalServerException(ErrorStatus._CHALLENGE_POT_NOT_FOUND));
        pot.payBonus(bonusAmount);

        potTransactionRepository.save(
            PotTransaction.builder()
                .type(PotTransactionType.BONUS_PAYOUT)
                .amount(bonusAmount)
                .balanceAfter(pot.getBalance())
                .settlement(settlement)
                .description(describePotTransaction(settlement))
                .build()
        );
    }

    // pot 원장 description을 "챌린지명_유저명_성공/실패" 형식으로 남겨 대시보드에서 바로 식별 가능하게 한다.
    private String describePotTransaction(Settlement settlement) {
        Participation participation = settlement.getParticipation();
        String resultLabel = participation.getStatus() == ParticipationStatus.SUCCESS ? "성공" : "실패";
        return "%s_%s_%s".formatted(
                participation.getChallenge().getName(),
                participation.getUser().getNickname(),
                resultLabel
        );
    }

    private Settlement createSettlement(
            Participation participation,
            BigDecimal rValue,
            int refundAmount,
            int depositRefundAmount,
            int bonusAmount
    ) {
        return Settlement.builder()
                .participation(participation)
                .depositAmount(participation.getDepositAmount())
                .rValue(rValue)
                .refundAmount(refundAmount)
                .depositRefundAmount(depositRefundAmount)
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
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        long balanceAfter = PointBalanceCalculator.addToBalance(user.getPointBalance(), refundAmount);

        user.setPointBalance(balanceAfter);

        ParticipationStatus resultStatus = participation.getStatus();
        pointTransactionRepository.save(
            PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.REFUND)
                .amount(refundAmount)
                .depositAmount(participation.getDepositAmount())
                .adjustmentAmount(adjustmentAmount)
                .balanceAfter(balanceAfter)
                .description(describeSettlementResult(participation.getChallenge().getName(), resultStatus))
                .resultStatus(resultStatus)
                .refType("SETTLEMENT")
                .refId(participation.getId())
                .build()
        );
    }

    // 포인트 트랜잭션 description에 챌린지명 + 성공/실패를 함께 남겨 사람이 봐도 결과를 바로 알 수 있게 한다.
    private String describeSettlementResult(String challengeName, ParticipationStatus status) {
        return "%s %s".formatted(challengeName, status == ParticipationStatus.SUCCESS ? "성공" : "실패");
    }

    private int calculateBonusAmount(int depositAmount, BigDecimal bonusRate) {
        return BigDecimal.valueOf(depositAmount)
                .multiply(bonusRate)
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
