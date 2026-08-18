package com.example.onuldo.domain.auth.controller;

import com.example.onuldo.domain.auth.controller.doc.AuthControllerDoc;
import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.EmailExistsResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.domain.auth.service.AuthService;
import com.example.onuldo.global.common.base.BaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
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
    public BaseResponse<OAuthResDto> oauthLogin(
            @Valid
            @RequestBody
            OAuthLoginReqDto request
    ) {
        return BaseResponse.onSuccess(authService.oauthLogin(request));
    }

    @PostMapping("/oauth/signup")
    public BaseResponse<AuthResDto> oauthSignup(
            @Valid
            @RequestBody
            OAuthSignupReqDto request
    ) {
        return BaseResponse.onSuccess(authService.oauthSignup(request));
    }

    @GetMapping("/email/exists")
    public BaseResponse<EmailExistsResDto> checkEmailExists(
            @NotBlank(message = "이메일은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9+_-]+(\\.[a-zA-Z0-9+_-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$",
                    message = "올바른 이메일 형식이 아닙니다."
            )
            @RequestParam
            String email
    ) {
        return BaseResponse.onSuccess(authService.checkEmailExists(email));
    }
}
