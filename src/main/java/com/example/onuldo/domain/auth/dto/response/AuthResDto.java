package com.example.onuldo.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResDto {
    private String accessToken;
    private String refreshToken;
}
