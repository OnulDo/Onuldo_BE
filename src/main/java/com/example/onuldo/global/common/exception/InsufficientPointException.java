package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.exception.code.BaseCodeInterface;
import lombok.Getter;

@Getter
public class InsufficientPointException extends RestApiException {

    private final PointErrorDetail errorDetail;

    public InsufficientPointException(BaseCodeInterface errorCode, long currentPoint, long requiredPoint) {
        super(errorCode);
        this.errorDetail = new PointErrorDetail(currentPoint, requiredPoint - currentPoint);
    }

    public record PointErrorDetail(long currentPoint, long shortage) {
    }
}
