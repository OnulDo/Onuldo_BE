package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class InvalidRequestException extends RestApiException {

    public InvalidRequestException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public InvalidRequestException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
