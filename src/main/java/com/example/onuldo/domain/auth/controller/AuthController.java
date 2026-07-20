package com.example.onuldo.domain.auth.controller;

import com.example.onuldo.domain.auth.controller.doc.AuthControllerDoc;
import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.domain.auth.service.AuthService;
import com.example.onuldo.global.common.base.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerDoc {

    private final AuthService authService;

    @PostMapping("/signup")
    public BaseResponse<AuthResDto> signup(
            @Valid
            @RequestBody
            EmailSignupReqDto request
    ) {
        return BaseResponse.onSuccess(authService.signup(request));
    }

    @PostMapping("/login")
    public BaseResponse<AuthResDto> login(
            @Valid
            @RequestBody
            EmailLoginReqDto request
    ) {
        return BaseResponse.onSuccess(authService.login(request));
    }

    @PostMapping("/refresh")
    public BaseResponse<AuthResDto> refresh(
            @Valid
            @RequestBody
            RefreshTokenReqDto request
    ) {
        return BaseResponse.onSuccess(authService.refresh(request));
    }

    @PostMapping("/oauth/login")
    public BaseResponse<OAuthResDto> oauth(
            @Valid
            @RequestBody
            OAuthReqDto request
    ) {
        return BaseResponse.onSuccess(authService.oauth(request));
    }
}
