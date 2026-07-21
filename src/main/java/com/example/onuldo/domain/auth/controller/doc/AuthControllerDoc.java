package com.example.onuldo.domain.auth.controller.doc;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "이메일 회원가입, 로그인, JWT 재발급 API")
public interface AuthControllerDoc {

    @Operation(
            summary = "이메일 회원가입",
            description = "이메일, 비밀번호, 닉네임으로 가입하고 accessToken 및 refreshToken을 발급합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "email": "onuldo@onuldo.com",
                          "password": "12341234",
                          "nickname": "오늘두",
                          "profileImageUrl": null,
                          "termAgreements": [
                             {
                                "termType": "SERVICE",
                                "value": true
                             },
                             {
                                "termType": "PRIVACY",
                                "value": true
                             },
                             {
                                "termType": "AGE_14",
                                "value": true
                             },
                             {
                                "termType": "MARKETING",
                                "value": false
                             }
                          ]
                        }
                        """
                    )
            )
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
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "email": "onuldo@onuldo.com",
                          "password": "12341234"
                        }
                        """
                    )
            )
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
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "refreshToken": "토큰은길어요그러니까여기에그냥붙여주면되고베어럴은안붙여도괜찮아요"
                        }
                        """
                    )
            )
    )
    BaseResponse<AuthResDto> refresh(
            @Valid
            @RequestBody
            RefreshTokenReqDto request
    );

    @Operation(
            summary = "소셜 로그인 (카카오/네이버)",
            description = "클라이언트가 SNS SDK로 발급받은 accessToken을 전달하면, " +
                    "최초 로그인 시 닉네임과 프로필 사진을 함께 전달받아 자동으로 회원가입을 진행하고 " +
                    "accessToken/refreshToken을 발급합니다. " +
                    "이때 isNewUser 값으로 신규 가입 여부도 함께 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                            "provider": "KAKAO",
                            "accessToken": "string",
                            "nickname": "오늘두",
                            "profileImageUrl": "string"
                        }
                        """
                    )
            )
    )
    BaseResponse<OAuthResDto> oauth(
            @Valid
            @RequestBody
            OAuthReqDto request
    );
}
