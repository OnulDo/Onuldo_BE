package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.support.NicknameValidator;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateProfileReqDto;
import com.example.onuldo.domain.user.dto.response.*;
import com.example.onuldo.domain.user.entity.NotificationSetting;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.repository.NotificationSettingRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NicknameValidator nicknameValidator;

    public GetProfileResDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        return GetProfileResDto.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public GetMyPageResDto getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

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
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

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

    private NotificationSetting getOrCreateSetting(Long userId) {
        return notificationSettingRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));
                    return notificationSettingRepository.save(
                            NotificationSetting.builder()
                                    .user(user)
                                    .build()
                    );
                });
    }
}
