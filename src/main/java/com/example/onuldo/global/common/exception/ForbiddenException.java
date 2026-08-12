package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class ForbiddenException extends RestApiException {

    public ForbiddenException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public ForbiddenException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
