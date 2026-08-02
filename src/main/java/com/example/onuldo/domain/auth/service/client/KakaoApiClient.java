package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.KakaoErrorResDto;
import com.example.onuldo.domain.auth.service.client.dto.KakaoUserInfoResDto;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoApiClient implements OAuthApiClient {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    // 카카오 API 공통 에러 코드 -401: 유효하지 않거나 만료된 토큰(AuthorizationException)
    private static final int INVALID_TOKEN_ERROR_CODE = -401;

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
                throw new RestApiException(GlobalErrorStatus._INVALID_SOCIAL_TOKEN);
            }
            throw new RestApiException(GlobalErrorStatus._OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("카카오 유저 정보 API 호출 중 오류", e);
            throw new RestApiException(GlobalErrorStatus._OAUTH_PROVIDER_ERROR);
        }
    }

    private boolean isInvalidTokenError(RestClientResponseException e) {
        if (!e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            return false;
        }
        try {
            KakaoErrorResDto error = e.getResponseBodyAs(KakaoErrorResDto.class);
            return error != null && Integer.valueOf(INVALID_TOKEN_ERROR_CODE).equals(error.code());
        } catch (RestClientException parseException) {
            return false;
        }
    }
}
