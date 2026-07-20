package com.example.onuldo.domain.auth.dto.request;

import com.example.onuldo.domain.user.enums.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthReqDto {

    @NotNull(message = "provider는 필수입니다.")
    private SocialProvider provider;

    @NotBlank(message = "accessToken은 필수입니다.")
    private String accessToken;
}
