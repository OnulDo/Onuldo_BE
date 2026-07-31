package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.KakaoUserInfoResDto;
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
public class KakaoApiClient implements OAuthApiClient {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

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
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        } catch (RestClientException e) {
            log.error("카카오 유저 정보 API 호출 중 오류", e);
            throw new RestApiException(GlobalErrorStatus._UNAUTHORIZED);
        }
    }
}
