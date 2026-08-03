package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;
import lombok.Getter;

@Getter
public class InsufficientPointException extends RestApiException {

    private final String errorDetail;

    public InsufficientPointException(BaseCodeInterface errorCode, long currentPoint, long requiredPoint) {
        super(errorCode);
        long shortage = requiredPoint - currentPoint;
        this.errorDetail = String.format("현재포인트 %d, 부족한 금액 %d", currentPoint, shortage);
    }
}
