package com.example.onuldo.global.common.exception.code.status;

import com.example.onuldo.global.common.exception.code.BaseCodeDto;
import com.example.onuldo.global.common.exception.code.BaseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorStatus implements BaseCodeInterface {
    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    _INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    _TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),
    _DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    _INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "이메일 또는 비밀번호가 올바르지 않습니다."),
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final boolean isSuccess = false;
    private final String code;
    private final String message;

    @Override
    public BaseCodeDto getCode() {
        return BaseCodeDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(isSuccess)
                .code(code)
                .message(message)
                .build();
    }
}
