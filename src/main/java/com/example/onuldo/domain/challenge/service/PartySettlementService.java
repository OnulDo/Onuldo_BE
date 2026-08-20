package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.SettlementStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.domain.user.support.PointBalanceCalculator;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * POI-07/08 파티 정산 (2026-08-07 개정: 일 단위 몰수·분배 모델 폐기, 일괄 정산 모델로 변경)
 */
@Service
@RequiredArgsConstructor
public class PartySettlementService {

    // 환급·분배 계산을 소수점 정확값으로 유지하기 위한 내부 계산 스케일
    private static final int CALC_SCALE = 10;
    private static final BigDecimal SUCCESS_THRESHOLD = BigDecimal.valueOf(0.85);
    // POI-07 전원 완주 보너스 b = 5% (개인 성공 보너스 2.5%와 다름)
    private static final BigDecimal BONUS_RATE = BigDecimal.valueOf(0.05);
    private final PartyRepository partyRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TimeService timeService;
    private final ChallengeNotificationService challengeNotificationService;

    /**
     * 파티 단위 1회 정산. 마지막 수행일 인증 마감 후에만 처리
     * 파티 락으로 동시 트리거(파티원별 인증 완료 + 파티 정산 스케줄러)를 직렬화
     */
    @Transactional
    public void settleParty(Long partyId) {
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTY_NOT_FOUND));

        // POI-08: 이미 정산된 파티면 재정산하지 않는다 (중복 정산·이중 지급 방어).
        if (settlementRepository.existsByParticipation_Party_Id(partyId)) {
            return;
        }

        List<Participation> participations =
                participationRepository.findAllByParty_IdAndStatusOrderByUser_IdAsc(partyId, ParticipationStatus.ONGOING);
        if (participations.isEmpty()) {
            return;
        }

        if (hasPendingManualReview(participations)) {
            return;
        }

        // 전원 마지막 날 PASS가 끝났으면 하루 챌린지는 인증 직후 정산한다.
        // 그 외에는 마지막 일 인증 마감 전이면 정산을 보류한다.
        if (!isAllMembersPassedOnEndDate(participations) && !isSettlementWindowClosed(participations)) {
            return;
        }

        processSettlement(participations);
        // 정산 완료 후 파티 상태를 확정해야 홈 "함께하는 파티"(status=ONGOING 필터)에서 빠진다.
        party.updateStatus(PartyStatus.FINISHED);
    }

    private boolean isSettlementWindowClosed(List<Participation> participations) {
        Participation reference = participations.get(0);
        LocalDate endDate = reference.getEndDate();
        LocalTime deadline = reference.getChallenge().getTimeEnd();
        var now = timeService.nowKst();

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

        return true;
    }

    private boolean hasPendingManualReview(List<Participation> participations) {
        List<Long> participationIds = participations.stream().map(Participation::getId).toList();
        return verificationRepository.existsPendingManualReviewByParticipationIds(participationIds);
    }

    private boolean isAllMembersPassedOnEndDate(List<Participation> participations) {
        LocalDate endDate = participations.get(0).getEndDate();
        return participations.stream()
                .allMatch(participation -> verificationRepository.existsByParticipation_IdAndVerificationDateAndReview(
                        participation.getId(), endDate, VerificationReviewStatus.PASS));
    }

    private void processSettlement(List<Participation> participations) {
        int memberCount = participations.size();
        List<MemberSettlementDraft> drafts = participations.stream()
                .map(this::createMemberDraft)
                .toList();

        BigDecimal failurePool = drafts.stream()
                .map(MemberSettlementDraft::shortfall)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successCount = drafts.stream()
                .filter(draft -> draft.status() == ParticipationStatus.SUCCESS)
                .count();
        boolean allSuccess = successCount == memberCount;

        // 실패금 풀을 성공 파티원에게 1/n 균등 분배. 성공자가 없으면(전원 실패) 분배 없이 운영자 회수.
        BigDecimal shareExact = successCount > 0
                ? failurePool.divide(BigDecimal.valueOf(successCount), CALC_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        for (MemberSettlementDraft draft : drafts) {
            settleMember(draft, shareExact, allSuccess);
        }
    }

    private MemberSettlementDraft createMemberDraft(Participation participation) {
        long totalDays = participation.getDurationDays();
        if (totalDays <= 0) {
            throw new InternalServerException(ErrorStatus._SETTLEMENT_INVALID_PERIOD);
        }

        long passDays = verificationRepository.countByParticipation_IdAndReview(
                participation.getId(), VerificationReviewStatus.PASS);

        // POI-07: 파티는 개인 챌린지와 동일한 달성률 공식을 쓰되, 주 1회 면제는 적용하지 않는다.
        BigDecimal rValue = BigDecimal.valueOf(passDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        ParticipationStatus status = rValue.compareTo(SUCCESS_THRESHOLD) >= 0
                ? ParticipationStatus.SUCCESS
                : ParticipationStatus.FAIL;

        int depositAmount = participation.getDepositAmount();
        BigDecimal ownRefundExact = status == ParticipationStatus.SUCCESS
                ? BigDecimal.valueOf(depositAmount)
                : calculateFailureRefundExact(depositAmount, rValue);
        BigDecimal shortfall = BigDecimal.valueOf(depositAmount).subtract(ownRefundExact);

        return new MemberSettlementDraft(participation, rValue, status, ownRefundExact, shortfall);
    }

    // 환급 = 도전금 × (r ÷ 85%). 몰수분(도전금 − 환급액)은 파티 실패금 풀로 보관했다가 종료 시 성공자에게 분배된다.
    private BigDecimal calculateFailureRefundExact(int depositAmount, BigDecimal rValue) {
        BigDecimal rate = rValue.divide(SUCCESS_THRESHOLD, CALC_SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(depositAmount).multiply(rate);
    }

    private void settleMember(MemberSettlementDraft draft, BigDecimal shareExact, boolean allSuccess) {
        Participation participation = draft.participation();
        boolean isSuccess = draft.status() == ParticipationStatus.SUCCESS;
        int depositAmount = participation.getDepositAmount();

        // 지급 항목별로 1P 미만 올림(단수 차액은 운영 부담, 유저 유리 원칙)해서 화면에 "도전금 환급"/"분배금"을
        // 각각 정확한 금액으로 나눠 보여줄 수 있게 한다. 항목별로 올림한 값을 그대로 더하므로 총액과도 항상 일치한다.
        int depositRefundAmount = draft.ownRefundExact().setScale(0, RoundingMode.CEILING).intValue();
        int partyShareAmount = isSuccess ? shareExact.setScale(0, RoundingMode.CEILING).intValue() : 0;
        // 개인 완주 보너스 없음 — 전원 완주(실패금 풀 없음)일 때만 파티 유일 보너스로 각자 도전금 × 5% 지급
        int bonusAmount = allSuccess
                ? BigDecimal.valueOf(depositAmount).multiply(BONUS_RATE).setScale(0, RoundingMode.HALF_UP).intValue()
                : 0;
        int payoutAmount = depositRefundAmount + partyShareAmount + bonusAmount;

        participation.changeStatus(draft.status());
        payoutPoint(participation, payoutAmount);

        settlementRepository.save(Settlement.builder()
                .participation(participation)
                .depositAmount(depositAmount)
                .rValue(draft.rValue())
                .refundAmount(payoutAmount)
                .depositRefundAmount(depositRefundAmount)
                .bonusAmount(bonusAmount)
                .partyShareAmount(partyShareAmount)
                .status(SettlementStatus.COMPLETED)
                .processedAt(timeService.nowKst())
                .build());
        challengeNotificationService.notifySettlementCompleted(participation, payoutAmount);
    }

    // 지갑 잔액 반영 + 원장 기록(REFUND, net) — 개인 환급과 동일한 방식으로 통일해 이중 지급 방어를 단순화
    private void payoutPoint(Participation participation, int payoutAmount) {
        User user = userRepository.findByIdForUpdate(participation.getUser().getId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        long balanceAfter = PointBalanceCalculator.addToBalance(user.getPointBalance(), payoutAmount);
        user.setPointBalance(balanceAfter);

        ParticipationStatus resultStatus = participation.getStatus();
        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.REFUND)
                .amount(payoutAmount)
                .depositAmount(participation.getDepositAmount())
                .adjustmentAmount(payoutAmount - participation.getDepositAmount())
                .balanceAfter(balanceAfter)
                .description(describeSettlementResult(participation.getChallenge().getName(), resultStatus))
                .resultStatus(resultStatus)
                .refType("SETTLEMENT")
                .refId(participation.getId())
                .build());
    }

    // 포인트 트랜잭션 description에 챌린지명 + 성공/실패를 함께 남겨 사람이 봐도 결과를 바로 알 수 있게 한다.
    private String describeSettlementResult(String challengeName, ParticipationStatus status) {
        return "%s %s".formatted(challengeName, status == ParticipationStatus.SUCCESS ? "성공" : "실패");
    }

    private record MemberSettlementDraft(
            Participation participation,
            BigDecimal rValue,
            ParticipationStatus status,
            BigDecimal ownRefundExact,
            BigDecimal shortfall
    ) {
    }
}
