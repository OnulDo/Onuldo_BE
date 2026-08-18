package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.KakaoErrorResDto;
import com.example.onuldo.domain.auth.service.client.dto.KakaoUserInfoResDto;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.UnauthorizedException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;

@Slf4j
@Component
public class KakaoApiClient implements OAuthApiClient {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    // 카카오 -401은 만료/위조된 액세스 토큰뿐 아니라 잘못된 app key, 토큰-앱 정보 불일치에도 동일하게 내려온다.
    // msg에 "token"이 있고 "app"(앱 설정 관련)이 없을 때만 토큰 자체 문제로 간주한다.
    private static final int INVALID_TOKEN_ERROR_CODE = -401;
    private static final String TOKEN_KEYWORD = "token";
    private static final String APP_KEYWORD = "app";

    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider supports() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String socialAccessToken) {
        try {
            KakaoUserInfoResDto response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header("Authorization", "Bearer " + socialAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResDto.class);

            return response.toOAuthUserInfo();
        } catch (RestClientResponseException e) {
            log.warn("카카오 유저 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (isInvalidTokenError(e)) {
                throw new UnauthorizedException(ErrorStatus._INVALID_SOCIAL_TOKEN);
            }
            throw new InternalServerException(ErrorStatus._OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("카카오 유저 정보 API 호출 중 오류", e);
            throw new InternalServerException(ErrorStatus._OAUTH_PROVIDER_ERROR);
        }
    }

    boolean isInvalidTokenError(RestClientResponseException e) {
        if (!e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            return false;
        }
        try {
            KakaoErrorResDto error = e.getResponseBodyAs(KakaoErrorResDto.class);
            if (error == null || !Integer.valueOf(INVALID_TOKEN_ERROR_CODE).equals(error.code()) || error.msg() == null) {
                return false;
            }
            String msg = error.msg().toLowerCase(Locale.ROOT);
            return msg.contains(TOKEN_KEYWORD) && !msg.contains(APP_KEYWORD);
        } catch (RestClientException parseException) {
            return false;
        }
    }
}
