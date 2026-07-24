package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionScrollResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public ChargePointResDto chargePoint(Long userId, ChargePointReqDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        long balanceAfter = user.getPointBalance() + request.point();
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.CHARGE)
                .amount(request.point())
                .balanceAfter(balanceAfter)
                .description("포인트 충전")
                .build()
        );

        return ChargePointResDto.builder()
                .amount(request.point())
                .balanceAfter(balanceAfter)
                .build();
    }

    @Transactional(readOnly = true)
    public PointWalletSummaryResDto getPointWalletSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        long totalDeposit = participationRepository.sumDepositAmountByUserIdAndStatusNot(userId, ParticipationStatus.ONGOING);
        long totalRefund = pointTransactionRepository.sumAmountByUserIdAndType(userId, PointTransactionType.REFUND);
        long totalPenalty = pointTransactionRepository.sumPenaltyAdjustmentByUserId(userId);
        long pendingPoints = participationRepository.sumDepositAmountByUserIdAndStatus(userId, ParticipationStatus.ONGOING);
        int averageReturnRate = totalDeposit == 0 ? 0 : Math.toIntExact(totalRefund * 100 / totalDeposit);

        return PointWalletSummaryResDto.builder()
                .balance(user.getPointBalance())
                .totalDeposit(totalDeposit)
                .totalRefund(totalRefund)
                .totalPenalty(totalPenalty)
                .averageReturnRate(averageReturnRate)
                .pendingPoints(pendingPoints)
                .build();
    }

    @Transactional(readOnly = true)
    public PointTransactionScrollResDto getPointTransactions(
            Long userId,
            PointTransactionType type,
            Long cursor,
            int size
    ) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserIdWithCursor(
                userId, type, cursor, PageRequest.of(0, size + 1)
        );

        boolean hasNext = transactions.size() > size;
        List<PointTransaction> content = hasNext ? transactions.subList(0, size) : transactions;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return PointTransactionScrollResDto.builder()
                .pointTransactions(content.stream().map(this::toPointTransactionResDto).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private PointTransactionResDto toPointTransactionResDto(PointTransaction pointTransaction) {
        return PointTransactionResDto.builder()
                .type(pointTransaction.getType())
                .title(pointTransaction.getDescription())
                .amount(pointTransaction.getAmount())
                .depositAmount(pointTransaction.getDepositAmount())
                .adjustmentAmount(pointTransaction.getAdjustmentAmount())
                .balanceAfter(pointTransaction.getBalanceAfter())
                .date(pointTransaction.getCreatedAt().toLocalDate())
                .build();
    }
}
