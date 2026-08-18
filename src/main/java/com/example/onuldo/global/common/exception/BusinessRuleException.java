package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class BusinessRuleException extends RestApiException {

    public BusinessRuleException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public BusinessRuleException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
