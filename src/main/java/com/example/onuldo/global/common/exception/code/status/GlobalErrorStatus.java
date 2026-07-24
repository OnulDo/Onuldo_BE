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
    _INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "로그인 또는 비밀번호가 일치하지 않습니다."),
    _LOGIN_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED", "로그인에 5회 실패하여 계정 로그인이 잠금되었습니다. 잠시 후 다시 시도해주세요."),
    _INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "사용할 수 없는 닉네임이에요."),
    _NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_SHORT", "2자 이상 입력해주세요."),
    _NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "NICKNAME_TOO_LONG", "8자 이내로 입력해주세요."),
    _TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS_REQUIRED", "필수 약관에 동의해주세요."),
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // 챌린지 관련 에러
    _CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "존재하지 않는 챌린지입니다."),
    _INVALID_DEPOSIT_OPTION(HttpStatus.BAD_REQUEST, "INVALID_DEPOSIT_OPTION", "선택할 수 없는 도전금입니다."),
    _INSUFFICIENT_POINT_FOR_CHALLENGE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT_FOR_CHALLENGE", "보유 포인트가 부족합니다."),
    _ALREADY_PARTICIPATING_CHALLENGE(HttpStatus.CONFLICT, "ALREADY_PARTICIPATING_CHALLENGE", "이미 참여 중인 챌린지입니다."),

    // Party 관련 에러
    _INSUFFICIENT_POINT_FOR_PARTY(HttpStatus.CONFLICT, "INSUFFICIENT_POINT_FOR_PARTY", "보유 포인트가 도전금보다 부족합니다."),
    _INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INVITE_CODE_GENERATION_FAILED", "초대코드 생성에 반복적으로 실패했습니다. 잠시 후 다시 시도해주세요."),
    _INVALID_PARTY_NAME(HttpStatus.BAD_REQUEST, "INVALID_PARTY_NAME", "파티 이름은 한글, 영문, 숫자, 공백으로 2~20자 이내로 입력해주세요."),
    _INVALID_MAX_MEMBERS(HttpStatus.BAD_REQUEST, "INVALID_MAX_MEMBERS", "모집 인원은 2명 이상 5명 이하로 설정해주세요.");

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
