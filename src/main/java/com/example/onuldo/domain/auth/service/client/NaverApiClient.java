package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.NaverUserInfoResDto;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class NaverApiClient implements OAuthApiClient {

    private static final String USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";

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
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        } catch (RestClientException e) {
            log.error("네이버 유저 정보 API 호출 중 오류", e);
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        }
    }
}
