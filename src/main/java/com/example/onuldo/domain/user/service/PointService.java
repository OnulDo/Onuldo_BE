package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.request.WithdrawPointReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.dto.response.WithdrawPointResDto;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.domain.user.support.PointBalanceCalculator;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.BusinessRuleException;
import com.example.onuldo.global.common.exception.DuplicateException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
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
    private static final int SIGNUP_BONUS = 100000;

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public ChargePointResDto chargePoint(Long userId, ChargePointReqDto request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        long balanceAfter = PointBalanceCalculator.addToBalance(user.getPointBalance(), request.point());
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
    public WithdrawPointResDto withdrawPoint(Long userId, WithdrawPointReqDto request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        if (user.getPointBalance() < request.point()) {
            throw new BusinessRuleException(ErrorStatus._INSUFFICIENT_POINT_FOR_WITHDRAW);
        }

        long balanceAfter = user.getPointBalance() - request.point();
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.WITHDRAW)
                .amount(request.point())
                .balanceAfter(balanceAfter)
                .description("포인트 출금")
                .build()
        );

        return WithdrawPointResDto.builder()
                .amount(request.point())
                .balanceAfter(balanceAfter)
                .build();

    }

    @Transactional
    public ChargePointResDto grantSignupBonus(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        if (pointTransactionRepository.existsByUser_IdAndDescription(userId, SIGNUP_BONUS_DESCRIPTION)) {
            throw new DuplicateException(ErrorStatus._SIGNUP_BONUS_ALREADY_GRANTED);
        }

        long balanceAfter = PointBalanceCalculator.addToBalance(user.getPointBalance(), SIGNUP_BONUS);
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.CHARGE)
                .amount(SIGNUP_BONUS)
                .balanceAfter(balanceAfter)
                .description(SIGNUP_BONUS_DESCRIPTION)
                .build()
        );

        return ChargePointResDto.builder()
                .amount(SIGNUP_BONUS)
                .balanceAfter(balanceAfter)
                .build();
    }

    @Transactional(readOnly = true)
    public PointWalletSummaryResDto getPointWalletSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

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
    public CursorPageResponse<PointTransactionResDto> getPointTransactions(
            Long userId,
            PointTransactionType type,
            String cursor,
            int size
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);

        Long lastId = CursorKeyCodec.isBlank(cursor) ? null : CursorKeyCodec.decodeAsLongs(cursor, 1)[0];

        List<PointTransaction> transactions = pointTransactionRepository.findByUserIdWithCursor(
                userId, type, lastId, CursorPageable.of(resolvedSize)
        );

        return CursorPageResponse.of(
                transactions,
                resolvedSize,
                this::toPointTransactionResDto,
                t -> CursorKeyCodec.encode(t.getId())
        );
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
            throw new InternalServerException(ErrorStatus._RETURN_RATE_OUT_OF_RANGE);
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
