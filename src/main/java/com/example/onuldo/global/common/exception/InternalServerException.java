package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class InternalServerException extends RestApiException {

    public InternalServerException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public InternalServerException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
