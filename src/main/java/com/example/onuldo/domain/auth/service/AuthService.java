package com.example.onuldo.domain.auth.service;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.OAuthSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.request.TermAgreementReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
import com.example.onuldo.domain.auth.dto.response.OAuthResDto;
import com.example.onuldo.domain.auth.entity.Term;
import com.example.onuldo.domain.auth.entity.TermAgreement;
import com.example.onuldo.domain.auth.entity.TermAgreementId;
import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.auth.repository.TermAgreementRepository;
import com.example.onuldo.domain.auth.repository.TermRepository;
import com.example.onuldo.domain.auth.service.client.dto.OAuthUserInfo;
import com.example.onuldo.domain.auth.support.NicknameValidator;
import com.example.onuldo.domain.user.entity.NotificationSetting;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.domain.user.enums.UserStatus;
import com.example.onuldo.domain.user.repository.NotificationSettingRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*\\p{Punct})[a-zA-Z0-9\\p{Punct}]+$"
    );
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 20;
    private static final int DEFAULT_PROFILE_IMAGE_MIN_NUMBER = 1;
    private static final int DEFAULT_PROFILE_IMAGE_MAX_NUMBER_EXCLUSIVE = 13;
    private static final Set<TermType> REQUIRED_TERM_TYPES = Set.of(
            TermType.SERVICE,
            TermType.PRIVACY,
            TermType.AGE_14
    );

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final TermAgreementRepository termAgreementRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final LoginFailureService loginFailureService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthService oAuthService;
    private final TimeService timeService;
    private final NicknameValidator nicknameValidator;

    @Transactional
    public AuthResDto signup(EmailSignupReqDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RestApiException(GlobalErrorStatus._DUPLICATE_EMAIL);
        }

        nicknameValidator.validate(request.nickname());
        validateRequiredTerms(request.termAgreements());
        validatePassword(request.password());

        User user = User.builder()
                .email(request.email())
                .nickname(request.nickname())
                .passwordHash(passwordEncoder.encode(request.password()))
                .socialProvider(SocialProvider.EMAIL)
                .emailVerified(false)
                .pointBalance(0L)
                .status(UserStatus.ACTIVE)
                .profileImageUrl(resolveProfileImageUrl(request.profileImageUrl()))
                .build();

        User savedUser = userRepository.save(user);
        saveTermAgreements(savedUser, request.termAgreements());
        saveDefaultNotificationSetting(savedUser);
        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthResDto login(EmailLoginReqDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._INVALID_LOGIN));

        LocalDateTime now = timeService.nowKst();
        if (isLocked(user, now)) {
            throw new RestApiException(GlobalErrorStatus._LOGIN_LOCKED);
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginFailureService.recordFailure(user.getId(), now);
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(timeService.nowKst());
        userRepository.save(user);
        return createAuthResponse(user);
    }

    public AuthResDto refresh(RefreshTokenReqDto request) {
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        return createAuthResponse(user);
    }

    @Transactional
    public OAuthResDto oauthLogin(OAuthLoginReqDto request) {
        OAuthUserInfo info = oAuthService.fetchUserInfo(request.provider(), request.socialAccessToken());

        Optional<User> existingUser = userRepository.findByEmail(info.email());
        if (existingUser.isEmpty()) {
            return OAuthResDto.builder()
                    .isNewUser(true)
                    .build();
        }

        User user = existingUser.get();
        if (user.getSocialProvider() != info.provider()) {
            throw new RestApiException(GlobalErrorStatus._DUPLICATE_EMAIL);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._INVALID_LOGIN);
        }

        user.setLastLoginAt(timeService.nowKst());
        userRepository.save(user);

        return OAuthResDto.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user))
                .refreshToken(jwtTokenProvider.createRefreshToken(user))
                .isNewUser(false)
                .build();
    }

    @Transactional
    public AuthResDto oauthSignup(OAuthSignupReqDto request) {
        OAuthUserInfo info = oAuthService.fetchUserInfo(request.provider(), request.socialAccessToken());

        if (userRepository.existsByEmail(info.email())) {
            throw new RestApiException(GlobalErrorStatus._DUPLICATE_EMAIL);
        }

        nicknameValidator.validate(request.nickname());
        validateRequiredTerms(request.termAgreements());

        User user = userRepository.save(User.builder()
                .email(info.email())
                .nickname(request.nickname())
                .profileImageUrl(resolveProfileImageUrl(request.profileImageUrl()))
                .socialId(info.socialId())
                .socialProvider(info.provider())
                .emailVerified(true)
                .pointBalance(0L)
                .status(UserStatus.ACTIVE)
                .lastLoginAt(timeService.nowKst())
                .build());

        saveTermAgreements(user, request.termAgreements());
        saveDefaultNotificationSetting(user);
        return createAuthResponse(user);
    }

    private AuthResDto createAuthResponse(User user) {
        return AuthResDto.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user))
                .refreshToken(jwtTokenProvider.createRefreshToken(user))
                .build();
    }

    private void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new RestApiException(GlobalErrorStatus._PASSWORD_TOO_SHORT);
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new RestApiException(GlobalErrorStatus._PASSWORD_TOO_LONG);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RestApiException(GlobalErrorStatus._INVALID_PASSWORD);
        }
    }

    private void validateRequiredTerms(List<TermAgreementReqDto> termAgreements) {
        if (termAgreements == null || termAgreements.isEmpty()) {
            throw new RestApiException(GlobalErrorStatus._TERMS_REQUIRED);
        }

        Map<TermType, Boolean> agreementMap = termAgreements.stream()
                .collect(Collectors.toMap(
                        TermAgreementReqDto::termType,
                        TermAgreementReqDto::value,
                        (first, second) -> second
                ));

        for (TermType requiredType : REQUIRED_TERM_TYPES) {
            if (!Boolean.TRUE.equals(agreementMap.get(requiredType))) {
                throw new RestApiException(GlobalErrorStatus._TERMS_REQUIRED);
            }
        }
    }

    private void saveDefaultNotificationSetting(User user) {
        notificationSettingRepository.save(
                NotificationSetting.builder()
                        .user(user)
                        .build()
        );
    }

    private void saveTermAgreements(User user, List<TermAgreementReqDto> termAgreements) {
        Map<TermType, Boolean> agreementMap = termAgreements.stream()
                .collect(
                        Collectors.toMap(
                                TermAgreementReqDto::termType,
                                TermAgreementReqDto::value,
                                (first, second) -> second
                        )
                );

        for (TermType requiredType : REQUIRED_TERM_TYPES) {
            termRepository.findByType(requiredType)
                    .ifPresent(term -> {
                        if (Boolean.TRUE.equals(agreementMap.get(requiredType))) {
                            termAgreementRepository.save(createTermAgreement(user, term, true));
                        }
                    });
        }

        termRepository.findByType(TermType.MARKETING)
                .ifPresent(term -> {
                    boolean agreed = Boolean.TRUE.equals(agreementMap.get(TermType.MARKETING));
                    termAgreementRepository.save(createTermAgreement(user, term, agreed));
                });
    }

    private TermAgreement createTermAgreement(User user, Term term, boolean agreed) {
        return TermAgreement.builder()
                .id(new TermAgreementId(user.getId(), term.getId()))
                .user(user)
                .term(term)
                .agreed(agreed)
                .build();
    }

    private String resolveProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            return profileImageUrl;
        }

        int imageNumber = ThreadLocalRandom.current().nextInt(
                DEFAULT_PROFILE_IMAGE_MIN_NUMBER,
                DEFAULT_PROFILE_IMAGE_MAX_NUMBER_EXCLUSIVE
        );
        return "default_asset:" + imageNumber;
    }

    private boolean isLocked(User user, LocalDateTime now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }
}
