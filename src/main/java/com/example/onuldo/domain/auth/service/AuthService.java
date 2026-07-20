package com.example.onuldo.domain.auth.service;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.domain.user.enums.UserStatus;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthService oAuthService;

    @Transactional
    public AuthResDto signup(EmailSignupReqDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RestApiException(GlobalErrorStatus._DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .nickname(request.nickname())
                .passwordHash(passwordEncoder.encode(request.password()))
                .socialProvider(SocialProvider.EMAIL)
                .emailVerified(false)
                .pointBalance(0L)
                .status(UserStatus.ACTIVE)
                .birthDate(request.birthDate())
                .build();

        User savedUser = userRepository.save(user);
        return createAuthResponse(savedUser);
    }

    public AuthResDto login(EmailLoginReqDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._INVALID_LOGIN));

        if (user.getPasswordHash() == null) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        return createAuthResponse(user);
    }

    public AuthResDto refresh(RefreshTokenReqDto request) {
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        return createAuthResponse(user);
    }

    @Transactional
    public OAuthResDto oauth(OAuthReqDto request) {
        OAuthUserInfo info = oAuthService.fetchUserInfo(request.getProvider(), request.getAccessToken());

        Optional<User> existingUser = userRepository.findByEmail(info.email());

        User user;
        boolean isNewUser;
        if (existingUser.isPresent()) {
            User found = existingUser.get();
            if (found.getSocialProvider() != info.provider()) {
                throw new RestApiException(GlobalErrorStatus._DUPLICATE_EMAIL);
            }
            user = found;
            isNewUser = false;
        } else {
            user = userRepository.save(User.builder()
                    .email(info.email())
                    .nickname(info.nickname())
                    .profileImageUrl(info.profileImageUrl())
                    .socialProvider(info.provider())
                    .emailVerified(true)
                    .pointBalance(0L)
                    .status(UserStatus.ACTIVE)
                    .build());
            isNewUser = true;
        }

        return OAuthResDto.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user))
                .refreshToken(jwtTokenProvider.createRefreshToken(user))
                .isNewUser(isNewUser)
                .build();
    }

    private AuthResDto createAuthResponse(User user) {
        return AuthResDto.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user))
                .refreshToken(jwtTokenProvider.createRefreshToken(user))
                .build();
    }
}
