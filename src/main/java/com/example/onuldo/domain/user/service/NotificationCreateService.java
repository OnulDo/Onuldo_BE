package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.repository.NotificationRepository;
import com.example.onuldo.domain.user.repository.NotificationSettingRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationCreateService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    // 알림함용 Notification을 중복 없이 생성하는 메서드
    @Transactional
    public Optional<Notification> createIfAbsent(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String refType,
            Long refId
    ) {
        if (!isNotificationEnabled(userId, type)) {
            return Optional.empty();
        }

        if (refType != null && refId != null) {
            var existing = notificationRepository.findFirstByUser_IdAndTypeAndRefTypeAndRefIdOrderByIdDesc(
                    userId,
                    type,
                    refType,
                    refId
            );
            if (existing.isPresent()) {
                return existing;
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        return Optional.of(notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .type(type)
                        .title(title)
                        .content(content)
                        .refType(refType)
                        .refId(refId)
                        .build()
        ));
    }

    private boolean isNotificationEnabled(Long userId, NotificationType type) {
        return notificationSettingRepository.findById(userId)
                .map(setting -> setting.isEnabled(type))
                .orElse(true);
    }
}
