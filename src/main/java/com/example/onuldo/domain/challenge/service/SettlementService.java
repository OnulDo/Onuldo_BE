package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.SettlementStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal FAILURE_THRESHOLD = BigDecimal.valueOf(0.25);
    private static final BigDecimal BONUS_RATE = BigDecimal.valueOf(0.05);

    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public void settleIfLastDay(Participation participation, LocalDate verificationDate) {
        if (!verificationDate.equals(participation.getEndDate())) {
            return;
        }

        if (settlementRepository.existsByParticipation_Id(participation.getId())) {
            return;
        }

        Participation lockedParticipation = participationRepository.findByIdForUpdate(participation.getId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTICIPATION_NOT_FOUND));

        long totalDays = lockedParticipation.getDurationWeeks() * 7L;
        long verifiedDays = verificationRepository.countByParticipation_Id(lockedParticipation.getId());
        long missingDays = totalDays - verifiedDays;

        BigDecimal missingRatio = BigDecimal.valueOf(missingDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        boolean success = missingRatio.compareTo(FAILURE_THRESHOLD) < 0;

        lockedParticipation.setStatus(success ? ParticipationStatus.SUCCESS : ParticipationStatus.FAIL);
        participationRepository.save(lockedParticipation);

        Settlement settlement = Settlement.builder()
                .participation(lockedParticipation)
                .depositAmount(lockedParticipation.getDepositAmount())
                .status(SettlementStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();

        if (success) {
            int bonusAmount = BigDecimal.valueOf(lockedParticipation.getDepositAmount())
                    .multiply(BONUS_RATE)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            int refundAmount = lockedParticipation.getDepositAmount() + bonusAmount;

            settlement = Settlement.builder()
                    .participation(lockedParticipation)
                    .depositAmount(lockedParticipation.getDepositAmount())
                    .refundAmount(refundAmount)
                    .bonusAmount(bonusAmount)
                    .partyShareAmount(0)
                    .status(SettlementStatus.COMPLETED)
                    .processedAt(LocalDateTime.now())
                    .build();

            User user = userRepository.findByIdForUpdate(lockedParticipation.getUser().getId())
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

            long balanceAfter = user.getPointBalance() + refundAmount;
            user.setPointBalance(balanceAfter);
            userRepository.save(user);

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(user)
                    .type(PointTransactionType.REFUND)
                    .amount(refundAmount)
                    .depositAmount(lockedParticipation.getDepositAmount())
                    .adjustmentAmount(bonusAmount)
                    .balanceAfter(balanceAfter)
                    .description(lockedParticipation.getChallenge().getName())
                    .refType("SETTLEMENT")
                    .refId(lockedParticipation.getId())
                    .build());
        }

        settlementRepository.save(settlement);
    }
}
