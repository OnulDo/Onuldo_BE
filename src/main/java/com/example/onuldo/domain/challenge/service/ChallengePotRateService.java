package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.ChallengePot;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ChallengePotRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 자립유지식: p_s × β ≤ p_f × L̄ → β_max = (p_f × L̄) ÷ p_s, 운영 β = β_max × (1 − 안전율 20%).
 * 실패자 몰수금만으로 성공자 보너스를 지급할 수 있는 상한선을 매주 재계산해 ChallengePot.currentBonusRate에 반영한다.
 * 이미 참가 중인 유저는 참가 시점에 고정한 Participation.appliedBonusRate를 쓰므로 소급 영향이 없다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengePotRateService {

    private static final Long CHALLENGE_POT_ID = 1L;
    private static final int AGGREGATION_WINDOW_DAYS = 30;
    private static final int CALC_SCALE = 10;
    private static final int RATE_SCALE = 4;
    private static final BigDecimal SAFETY_MARGIN = BigDecimal.valueOf(0.2);
    private static final BigDecimal MIN_BONUS_RATE = BigDecimal.valueOf(0.01);
    private static final BigDecimal MAX_BONUS_RATE = BigDecimal.valueOf(0.10);

    private final SettlementRepository settlementRepository;
    private final ChallengePotRepository challengePotRepository;
    private final TimeService timeService;

    @Transactional
    public void recalculateBonusRate() {
        var since = timeService.nowKst().minusDays(AGGREGATION_WINDOW_DAYS);

        List<Settlement> successSettlements = settlementRepository
                .findPersonalSettlementsByParticipationStatusSince(ParticipationStatus.SUCCESS, since);
        List<Settlement> failSettlements = settlementRepository
                .findPersonalSettlementsByParticipationStatusSince(ParticipationStatus.FAIL, since);

        long successCount = successSettlements.size();
        long failCount = failSettlements.size();
        long totalCount = successCount + failCount;

        // 성공 표본이 없으면 p_s가 0이 되어 나눗셈이 불가능하다 — 이번 주는 갱신을 건너뛰고 기존 β를 유지한다.
        if (successCount == 0 || totalCount == 0) {
            log.info(
                    "recalculateBonusRate skipped: insufficient sample. successCount={}, failCount={}",
                    successCount, failCount
            );
            return;
        }

        BigDecimal pSuccess = BigDecimal.valueOf(successCount)
                .divide(BigDecimal.valueOf(totalCount), CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal pFail = BigDecimal.valueOf(failCount)
                .divide(BigDecimal.valueOf(totalCount), CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal avgForfeitureRate = averageForfeitureRate(failSettlements);

        // β_max = (p_f × L̄) ÷ p_s
        BigDecimal betaMax = pFail.multiply(avgForfeitureRate)
                .divide(pSuccess, CALC_SCALE, RoundingMode.HALF_UP);

        // 운영 β = β_max × (1 − 안전율)
        BigDecimal operatingBeta = betaMax.multiply(BigDecimal.ONE.subtract(SAFETY_MARGIN));

        BigDecimal clampedBeta = operatingBeta
                .max(MIN_BONUS_RATE)
                .min(MAX_BONUS_RATE)
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);

        ChallengePot pot = challengePotRepository.findByIdForUpdate(CHALLENGE_POT_ID)
                .orElseThrow(() -> new InternalServerException(ErrorStatus._CHALLENGE_POT_NOT_FOUND));
        pot.updateCurrentBonusRate(clampedBeta);

        log.info(
                "recalculateBonusRate updated. successCount={}, failCount={}, pSuccess={}, pFail={}, "
                        + "avgForfeitureRate={}, betaMax={}, appliedBeta={}",
                successCount, failCount, pSuccess, pFail, avgForfeitureRate, betaMax, clampedBeta
        );
    }

    // L̄ = 실패 정산들의 평균 몰수율 = mean((depositAmount - depositRefundAmount) / depositAmount)
    private BigDecimal averageForfeitureRate(List<Settlement> failSettlements) {
        if (failSettlements.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalForfeitureRate = failSettlements.stream()
                .map(this::forfeitureRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalForfeitureRate.divide(BigDecimal.valueOf(failSettlements.size()), CALC_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal forfeitureRate(Settlement settlement) {
        int depositAmount = settlement.getDepositAmount();
        int forfeitedAmount = depositAmount - settlement.getDepositRefundAmount();

        return BigDecimal.valueOf(forfeitedAmount)
                .divide(BigDecimal.valueOf(depositAmount), CALC_SCALE, RoundingMode.HALF_UP);
    }
}
