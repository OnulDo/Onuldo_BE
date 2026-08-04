package com.example.onuldo.global.common.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import com.example.onuldo.global.common.time.TimeService;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"timestamp", "code", "message", "result"}) // JSON 응답 시 순서를 정의
public class BaseResponse<T> {

    private final LocalDateTime timestamp = TimeService.nowKstStatic();
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    //성공한 경우 응답 생성 (기본 메시지)
    public static <T> BaseResponse<T> onSuccess(T result) {
        return new BaseResponse<>("SUCCESS", "요청에 성공하였습니다.", result);
    }

    //성공한 경우 응답 생성 (커스텀 메시지)
    public static <T> BaseResponse<T> onSuccess(String message, T result) {
        return new BaseResponse<>("SUCCESS", message, result);
    }

    // 실패한 경우 응답 생성
    public static <T> BaseResponse<T> onFailure(String code, String message, T data) {
        return new BaseResponse<>(code, message, data);
    }

}
