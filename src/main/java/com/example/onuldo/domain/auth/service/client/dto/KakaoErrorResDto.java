package com.example.onuldo.domain.auth.service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoErrorResDto(
        Integer code,
        String msg
) {
}
