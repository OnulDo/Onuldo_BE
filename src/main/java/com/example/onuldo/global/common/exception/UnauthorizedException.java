package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class UnauthorizedException extends RestApiException {

    public UnauthorizedException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
