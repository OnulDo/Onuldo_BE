package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.NaverUserInfoResDto;
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
public class NaverApiClient implements OAuthApiClient {

    private static final String USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";
    // 네이버 프로필 API 오류 코드 024: 인증 실패(만료/위조된 토큰). 028(인증 헤더 누락) 등은 토큰 자체의 오류가 아니므로 제외
    private static final String INVALID_TOKEN_RESULT_CODE = "024";

    private final RestClient restClient = RestClient.create();

    @Override
    public SocialProvider supports() {
        return SocialProvider.NAVER;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String socialAccessToken) {
        try {
            NaverUserInfoResDto response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header("Authorization", "Bearer " + socialAccessToken)
                    .retrieve()
                    .body(NaverUserInfoResDto.class);

            return response.toOAuthUserInfo();
        } catch (RestClientResponseException e) {
            log.warn("네이버 유저 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (isInvalidTokenError(e)) {
                throw new RestApiException(GlobalErrorStatus._INVALID_SOCIAL_TOKEN);
            }
            throw new RestApiException(GlobalErrorStatus._OAUTH_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.error("네이버 유저 정보 API 호출 중 오류", e);
            throw new RestApiException(GlobalErrorStatus._OAUTH_PROVIDER_ERROR);
        }
    }

    private boolean isInvalidTokenError(RestClientResponseException e) {
        if (!e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            return false;
        }
        try {
            NaverUserInfoResDto error = e.getResponseBodyAs(NaverUserInfoResDto.class);
            return error != null && INVALID_TOKEN_RESULT_CODE.equals(error.resultcode());
        } catch (RestClientException parseException) {
            return false;
        }
    }
}
