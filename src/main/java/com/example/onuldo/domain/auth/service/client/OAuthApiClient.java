package com.example.onuldo.domain.auth.service.client;

import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.enums.SocialProvider;

public interface OAuthApiClient {

    SocialProvider supports();

    OAuthUserInfo fetchUserInfo(String socialAccessToken);
}
