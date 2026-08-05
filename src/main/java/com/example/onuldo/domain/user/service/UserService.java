package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.support.NicknameValidator;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateProfileReqDto;
import com.example.onuldo.domain.user.dto.response.GetMyPageResDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.GetProfileResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateProfileResDto;
import com.example.onuldo.domain.user.entity.NotificationSetting;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.UserStatus;
import com.example.onuldo.domain.user.repository.NotificationSettingRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final ParticipationRepository participationRepository;
    private final NicknameValidator nicknameValidator;
    private final TimeService timeService;

    public GetProfileResDto getProfile(Long userId) {
        User user = getActiveUser(userId);

        return GetProfileResDto.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public GetMyPageResDto getMyPage(Long userId) {
        User user = getActiveUser(userId);

        return GetMyPageResDto.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .currentPoint(user.getPointBalance())
                .joinedAt(user.getCreatedAt().toLocalDate())
                .build();
    }

    @Transactional
    public UpdateProfileResDto updateProfile(Long userId, UpdateProfileReqDto request) {
        User user = getActiveUserForUpdate(userId);

        boolean hasProfileImageUrl = request.profileImageUrl() != null && !request.profileImageUrl().isBlank();

        if (request.nickname() == null && !hasProfileImageUrl) {
            throw new RestApiException(GlobalErrorStatus._PROFILE_UPDATE_FIELD_REQUIRED);
        }

        if (request.nickname() != null) {
            nicknameValidator.validate(request.nickname());
            user.setNickname(request.nickname());
        }

        if (hasProfileImageUrl) {
            user.setProfileImageUrl(request.profileImageUrl());
        }

        return UpdateProfileResDto.builder()
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

    }

    @Transactional
    public GetNotificationResDto getNotification(Long userId) {
        NotificationSetting setting = getOrCreateSetting(userId);
        return GetNotificationResDto.builder()
                .allEnabled(setting.getAllEnabled())
                .verificationDeadline(setting.getVerificationDeadline())
                .verificationResult(setting.getVerificationResult())
                .challengeStart(setting.getChallengeStart())
                .refundComplete(setting.getRefundComplete())
                .deductionAlert(setting.getDeductionAlert())
                .build();
    }

    @Transactional
    public UpdateNotificationResDto updateNotification(Long userId, UpdateNotificationReqDto request) {
        NotificationSetting setting = getOrCreateSetting(userId);
        setting.apply(request.type(), request.enabled());

        return UpdateNotificationResDto.builder()
                .type(request.type())
                .enabled(request.enabled())
                .build();
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getActiveUserForUpdate(userId);

        if (participationRepository.existsByUser_IdAndStatus(userId, ParticipationStatus.ONGOING)) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "진행 중인 챌린지가 있어 회원 탈퇴할 수 없습니다.");
        }

        user.setStatus(UserStatus.WITHDRAWN);
        user.setWithdrawalRequestedAt(timeService.nowKst());
    }

    private NotificationSetting getOrCreateSetting(Long userId) {
        return notificationSettingRepository.findById(userId)
                .orElseGet(() -> {
                    User user = getActiveUser(userId);
                    return notificationSettingRepository.save(
                            NotificationSetting.builder()
                                    .user(user)
                                    .build()
                    );
                });
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._USER_NOT_FOUND);
        }

        return user;
    }

    private User getActiveUserForUpdate(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RestApiException(GlobalErrorStatus._USER_NOT_FOUND);
        }

        return user;
    }
}
