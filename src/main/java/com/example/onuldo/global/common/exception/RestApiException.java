package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public abstract class RestApiException extends RuntimeException {

    private final BaseCodeInterface errorCode; //추상화 시킨 인터페이스를 받아서 사용
    private final String detailMessage;

    protected RestApiException(BaseCodeInterface errorCode) {
        this(errorCode, null);
    }

    protected RestApiException(BaseCodeInterface errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    //추상화 시킨 ErrorCode의 getrCode()를 사용하여 ErrorCode를 반환
    public BaseCodeDto getErrorCode() {
        return this.errorCode.getCode();
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
