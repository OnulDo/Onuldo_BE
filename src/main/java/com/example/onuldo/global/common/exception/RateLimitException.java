package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class RateLimitException extends RestApiException {

    public RateLimitException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public RateLimitException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
