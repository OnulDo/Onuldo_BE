package com.example.onuldo.global.common.exception;

import com.example.onuldo.global.common.alert.DiscordAlertSender;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ExceptionAdvice {

    private final DiscordAlertSender discordAlertSender;

    /*
     * 포인트 부족 예외 처리 (errorDetail에 현재 포인트, 부족 금액 포함)
     */
    @ExceptionHandler(value = InsufficientPointException.class)
    public ResponseEntity<BaseResponse<String>> handleInsufficientPointException(
            InsufficientPointException e
    ) {
        BaseCodeDto errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), e.getErrorDetail()));
    }

    /*
     * 직접 정의한 RestApiException 에러 클래스에 대한 예외 처리
     */
    // @ExceptionHandler는 Controller계층에서 발생하는 에러를 잡아서 메서드로 처리해주는 기능
    @ExceptionHandler(value = RestApiException.class)
    public ResponseEntity<BaseResponse<String>> handleRestApiException(RestApiException e) {
        BaseCodeDto errorCode = e.getErrorCode();
        if (e.getDetailMessage() != null) {
            return ResponseEntity
                    .status(errorCode.getHttpStatus().value())
                    .body(BaseResponse.onFailure(errorCode.getCode(), e.getDetailMessage(), null));
        }
        return handleExceptionInternal(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return handleExceptionInternalObject(GlobalErrorStatus._BAD_REQUEST.getCode(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first
                ));

        return handleExceptionInternalObject(GlobalErrorStatus._BAD_REQUEST.getCode(), errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<BaseResponse<String>> handleHandlerMethodValidationException() {
        return handleExceptionInternal(GlobalErrorStatus._BAD_REQUEST.getCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<String>> handleHttpMessageNotReadableException() {
        return handleExceptionInternal(GlobalErrorStatus._BAD_REQUEST.getCode());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<String>> handleMethodArgumentTypeMismatchException() {
        return handleExceptionInternal(GlobalErrorStatus._BAD_REQUEST.getCode());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<String>> handleMissingServletRequestParameterException() {
        return handleExceptionInternal(GlobalErrorStatus._BAD_REQUEST.getCode());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<String>> handleHttpRequestMethodNotSupportedException() {
        return handleExceptionInternal(GlobalErrorStatus._METHOD_NOT_ALLOWED.getCode());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<BaseResponse<String>> handleHttpMediaTypeNotSupportedException() {
        return handleExceptionInternal(GlobalErrorStatus._UNSUPPORTED_MEDIA_TYPE.getCode());
    }

    /*
     * 일반적인 서버 에러에 대한 예외 처리
     */
    @ExceptionHandler
    public ResponseEntity<BaseResponse<String>> handleException(Exception e, HttpServletRequest request) {
        e.printStackTrace(); //예외 정보 출력

        // 처리되지 않은 진짜 500 에러만 디스코드로 알림 (비즈니스 예외/검증 오류는 위에서 이미 처리됨)
        // 여기서 다루지 않는 Spring의 나머지 4xx 예외(예: 406 등)까지 이 핸들러로 떨어질 수 있으므로 서버 에러만 걸러서 알림한다.
        if (isServerError(e)) {
            discordAlertSender.sendServerError(request.getRequestURI(), e);
        }

        return handleExceptionInternalFalse(GlobalErrorStatus._INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
    }

    // 예외가 4xx 클라이언트 오류로 명시된 경우가 아니면 서버 에러(5xx)로 간주한다.
    private boolean isServerError(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode().is5xxServerError();
        }
        return true;
    }

    private ResponseEntity<BaseResponse<String>> handleExceptionInternal(BaseCodeDto errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), null));
    }

    private ResponseEntity<Object> handleExceptionInternalObject(BaseCodeDto errorCode, Object errorArgs) {
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), errorArgs));
    }

    private ResponseEntity<BaseResponse<String>> handleExceptionInternalFalse(BaseCodeDto errorCode,
                                                                              String errorPoint) {
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), errorPoint));
    }
}
