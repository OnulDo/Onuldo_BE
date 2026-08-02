package com.example.onuldo.domain.auth.service;

import com.example.onuldo.domain.auth.service.client.OAuthApiClient;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuthService {

    private final Map<SocialProvider, OAuthApiClient> clients;

    public OAuthService(List<OAuthApiClient> apiClients) {
        this.clients = apiClients.stream()
                .collect(Collectors.toMap(OAuthApiClient::supports, Function.identity()));
    }

    public OAuthUserInfo fetchUserInfo(SocialProvider provider, String socialAccessToken) {
        OAuthApiClient client = clients.get(provider);
        if (client == null) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST);
        }
        return client.fetchUserInfo(socialAccessToken);
    }
}
