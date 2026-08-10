package com.example.onuldo.domain.auth.controller.doc;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.EmailExistsResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "이메일 회원가입, 로그인, JWT 재발급 API")
public interface AuthControllerDoc {

    @Operation(
            summary = "이메일 회원가입",
            description = "이메일, 비밀번호, 닉네임으로 가입하고 accessToken 및 refreshToken을 발급합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
    )
    BaseResponse<AuthResDto> refresh(
            @Valid
            @RequestBody
            RefreshTokenReqDto request
    );

    @Operation(
            summary = "소셜 로그인 (카카오)",
            description = "클라이언트가 SNS SDK로 발급받은 socialAccessToken을 전달하면, " +
                    "가입 여부를 확인하여 이미 가입된 유저라면 accessToken/refreshToken을 발급합니다. " +
                    "아직 가입되지 않은 유저라면 토큰 없이 isNewUser=true만 반환하며, " +
                    "이 경우 클라이언트는 oauth/signup API를 호출해 회원가입을 진행해야 합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
    )
    BaseResponse<OAuthResDto> oauthLogin(
            @Valid
            @RequestBody
            OAuthLoginReqDto request
    );

    @Operation(
            summary = "소셜 회원가입 (카카오)",
            description = "oauth/login에서 isNewUser=true를 받은 경우 호출합니다. " +
                    "SNS SDK로 발급받은 socialAccessToken과 닉네임, 프로필 사진, 약관 동의 여부를 전달받아 " +
                    "회원가입을 진행하고 accessToken/refreshToken을 발급합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
    )
    BaseResponse<AuthResDto> oauthSignup(
            @Valid
            @RequestBody
            OAuthSignupReqDto request
    );

    @Operation(
            summary = "이메일 중복 확인",
            description = """
                    회원가입 폼 입력 중 해당 이메일이 이미 가입되어 있는지 확인합니다.
                    실제 가입 시점의 중복 여부는 signup API에서 별도로 최종 검증합니다.
                    """
    )
    BaseResponse<EmailExistsResDto> checkEmailExists(
            @Parameter(description = "확인할 이메일", example = "onuldo@onuldo.com")
            @NotBlank(message = "이메일은 필수입니다.")
            @Pattern(
                    regexp = "^[a-zA-Z0-9+_-]+(\\.[a-zA-Z0-9+_-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$",
                    message = "올바른 이메일 형식이 아닙니다."
            )
            @RequestParam
            String email
    );
}
