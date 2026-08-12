package com.example.onuldo.domain.user.support;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.BusinessRuleException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;

public final class PointBalanceCalculator {

    private PointBalanceCalculator() {
    }

    public static long addToBalance(long currentBalance, long amount) {
        try {
            return Math.addExact(currentBalance, amount);
        } catch (ArithmeticException e) {
            throw new BusinessRuleException(ErrorStatus._POINT_BALANCE_OVERFLOW);
        }
    }
}
