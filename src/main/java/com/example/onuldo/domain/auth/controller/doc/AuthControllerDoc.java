package com.example.onuldo.domain.auth.controller.doc;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "이메일 회원가입, 로그인, JWT 재발급 API")
public interface AuthControllerDoc {

    @Operation(
            summary = "이메일 회원가입",
            description = "이메일, 비밀번호, 닉네임으로 가입하고 accessToken 및 refreshToken을 발급합니다."
    )
    BaseResponse<AuthResDto> signup(
            @Valid
            @RequestBody
            EmailSignupReqDto request
    );

    @Operation(
            summary = "이메일 로그인",
            description = "이메일과 비밀번호로 로그인하고 accessToken 및 refreshToken을 발급합니다."
    )
    BaseResponse<AuthResDto> login(
            @Valid
            @RequestBody
            EmailLoginReqDto request
    );

    @Operation(
            summary = "JWT 재발급",
            description = "refreshToken으로 새로운 accessToken 및 refreshToken을 발급합니다."
    )
    BaseResponse<AuthResDto> refresh(
            @Valid
            @RequestBody
            RefreshTokenReqDto request
    );
}
