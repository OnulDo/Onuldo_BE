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

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private static final int PERCENT_MULTIPLIER = 100;
    private static final String SIGNUP_BONUS_DESCRIPTION = "신규 회원 가입 포인트 지급";

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public ChargePointResDto chargePoint(Long userId, ChargePointReqDto request) {
        User user = userRepository.findByIdForUpdate(userId)
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

    @Transactional
    public ChargePointResDto grantSignupBonus(Long userId, ChargePointReqDto request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        if (pointTransactionRepository.existsByUser_IdAndDescription(userId, SIGNUP_BONUS_DESCRIPTION)) {
            throw new RestApiException(GlobalErrorStatus._SIGNUP_BONUS_ALREADY_GRANTED);
        }

        long balanceAfter = user.getPointBalance() + request.point();
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.CHARGE)
                .amount(request.point())
                .balanceAfter(balanceAfter)
                .description(SIGNUP_BONUS_DESCRIPTION)
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

        long totalDeposit = participationRepository.sumDepositAmountByUserIdAndStatusNot(
                userId,
                ParticipationStatus.ONGOING
        );
        long totalRefund = pointTransactionRepository.sumAmountByUserIdAndType(userId, PointTransactionType.REFUND);
        long totalPenalty = pointTransactionRepository.sumPenaltyAdjustmentByUserId(userId);
        long pendingPoints = participationRepository.sumDepositAmountByUserIdAndStatus(
                userId,
                ParticipationStatus.ONGOING
        );
        int averageReturnRate = calculateAverageReturnRate(totalDeposit, totalRefund);

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
        validateTransactionPageSize(size);

        List<PointTransaction> transactions = pointTransactionRepository.findByUserIdWithCursor(
                userId, type, cursor, PageRequest.of(0, size + 1)
        );

        boolean hasNext = transactions.size() > size;
        List<PointTransaction> content = hasNext ? transactions.subList(0, size) : transactions;
        Long nextCursor = hasNext ? content.getLast().getId() : null;

        return PointTransactionScrollResDto.builder()
                .pointTransactions(content.stream().map(this::toPointTransactionResDto).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private int calculateAverageReturnRate(long totalDeposit, long totalRefund) {
        if (totalDeposit == 0) {
            return 0;
        }

        BigInteger averageReturnRate = BigInteger.valueOf(totalRefund)
                .multiply(BigInteger.valueOf(PERCENT_MULTIPLIER))
                .divide(BigInteger.valueOf(totalDeposit));

        try {
            return averageReturnRate.intValueExact();
        } catch (ArithmeticException e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "평균 환급률이 허용 범위를 초과했습니다.");
        }
    }

    private void validateTransactionPageSize(int size) {
        if (size <= 0) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "페이지 크기는 1 이상이어야 합니다.");
        }
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
