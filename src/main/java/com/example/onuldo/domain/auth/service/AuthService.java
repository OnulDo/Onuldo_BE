package com.example.onuldo.domain.auth.service;

import com.example.onuldo.domain.auth.dto.request.EmailLoginReqDto;
import com.example.onuldo.domain.auth.dto.request.EmailSignupReqDto;
import com.example.onuldo.domain.auth.dto.request.RefreshTokenReqDto;
import com.example.onuldo.domain.auth.dto.response.AuthResDto;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    private AuthResDto createAuthResponse(User user) {
        return AuthResDto.builder()
                .accessToken(jwtTokenProvider.createAccessToken(user))
                .refreshToken(jwtTokenProvider.createRefreshToken(user))
                .build();
    }

    private void validateNickname(String nickname) {
        if (nickname.length() < 2) {
            throw new RestApiException(GlobalErrorStatus._NICKNAME_TOO_SHORT);
        }

        if (nickname.length() > 8) {
            throw new RestApiException(GlobalErrorStatus._NICKNAME_TOO_LONG);
        }

        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new RestApiException(GlobalErrorStatus._INVALID_NICKNAME);
        }

        for (String bannedWord : NicknameBannedWords.VALUES) {
            if (nickname.contains(bannedWord.toLowerCase(Locale.ROOT))) {
                throw new RestApiException(GlobalErrorStatus._INVALID_NICKNAME);
            }
        }
    }

    private void validateRequiredTerms(List<TermAgreementReqDto> termAgreements) {
        if (termAgreements == null || termAgreements.isEmpty()) {
            throw new RestApiException(GlobalErrorStatus._TERMS_REQUIRED);
        }

        Map<Integer, Boolean> agreementMap = termAgreements.stream()
                .collect(Collectors.toMap(
                        TermAgreementReqDto::termId,
                        TermAgreementReqDto::value,
                        (first, second) -> second
                ));

        for (TermType requiredType : REQUIRED_TERM_TYPES) {
            Term term = termRepository.findByType(requiredType)
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._TERMS_REQUIRED));

            if (!Boolean.TRUE.equals(agreementMap.get(term.getId()))) {
                throw new RestApiException(GlobalErrorStatus._TERMS_REQUIRED);
            }
        }
    }

    private void saveTermAgreements(User user, List<TermAgreementReqDto> termAgreements) {
        Map<Integer, Boolean> agreementMap = termAgreements.stream()
                .collect(
                        Collectors.toMap(
                                TermAgreementReqDto::termId,
                                TermAgreementReqDto::value,
                                (first, second) -> second
                        )
                );

        for (TermType requiredType : REQUIRED_TERM_TYPES) {
            termRepository.findByType(requiredType)
                    .ifPresent(term -> {
                        if (Boolean.TRUE.equals(agreementMap.get(term.getId()))) {
                            termAgreementRepository.save(createTermAgreement(user, term, true));
                        }
                    });
        }

        termRepository.findByType(TermType.MARKETING)
                .ifPresent(term -> {
                    boolean agreed = Boolean.TRUE.equals(agreementMap.get(term.getId()));
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

        // profileImageUrl이 null인 경우 기본 캐릭터 이미지 1~12중 랜덤 배정
        int imageNumber = ThreadLocalRandom.current().nextInt(1, 13);
        return "default_asset:" + imageNumber;
    }

    /** 계정 잠금 여부 반환
     * */
    private boolean isLocked(User user, LocalDateTime now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    /** 로그인 실패 시 처리 메서드
     * */
    private void handleLoginFailure(User user, LocalDateTime now) {
        // 로그인 잠금 기한 종료 후 재요청 시 count 재설정
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setLockedUntil(null);
            user.setLoginFailCount(0);
        }

        LocalDateTime lockedUntil = null;
        if (user.getLoginFailCount() != null && user.getLoginFailCount() + 1 >= MAX_LOGIN_FAILURE_COUNT) {
            lockedUntil = now.plusSeconds(LOCK_SECONDS);
        }
        user.setLoginFailCount(user.getLoginFailCount() == null ? 1 : user.getLoginFailCount() + 1);
        user.setLockedUntil(lockedUntil);
    }
}
