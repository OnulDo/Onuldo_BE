package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class NotFoundException extends RestApiException {

    public NotFoundException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public NotFoundException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
