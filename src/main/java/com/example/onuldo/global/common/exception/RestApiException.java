package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class RestApiException extends RuntimeException {

    private final BaseCodeInterface errorCode; //추상화 시킨 인터페이스를 받아서 사용
    private final String detailMessage;

    public RestApiException(BaseCodeInterface errorCode) {
        this(errorCode, null);
    }

    public RestApiException(BaseCodeInterface errorCode, String detailMessage) {
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public static RestApiException insufficientPoint(
            BaseCodeInterface errorCode,
            long currentPoint,
            long requiredPoint
    ){
        long shortage = requiredPoint - currentPoint;
        return new RestApiException(
                errorCode,
                String.format("보유 포인트가 부족합니다. 현재 포인트: %dP, 부족한 금액: %dP", currentPoint, shortage)
        );
    }

    //추상화 시킨 ErrorCode의 getrCode()를 사용하여 ErrorCode를 반환
    public BaseCodeDto getErrorCode() {
        return this.errorCode.getCode();
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
