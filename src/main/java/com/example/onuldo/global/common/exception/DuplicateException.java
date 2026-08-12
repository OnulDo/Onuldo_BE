package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;

public class DuplicateException extends RestApiException {

    public DuplicateException(BaseCodeInterface errorCode) {
        super(errorCode);
    }

    public DuplicateException(BaseCodeInterface errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
