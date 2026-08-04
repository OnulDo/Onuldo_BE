package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import com.example.onuldo.domain.auth.repository.DeviceLogRepository;
import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.repository.NotificationDispatchRepository;
import com.example.onuldo.domain.user.repository.NotificationRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDispatchService {

    private final NotificationDispatchRepository notificationDispatchRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DeviceLogRepository deviceLogRepository;
    private final FcmPushService fcmPushService;
    private final TimeService timeService;

    @Transactional
    public NotificationDispatch enqueue(
            Long userId,
            NotificationType type,
            String title,
            String content,
            LocalDateTime scheduledAt,
            Long notificationId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        Notification notification;
        if (notificationId != null) {
            notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._BAD_REQUEST));
        } else {
            notification = notificationRepository.save(
                    Notification.builder()
                            .user(user)
                            .type(type)
                            .title(title)
                            .content(content)
                            .build()
            );
        }

        return notificationDispatchRepository.save(
                NotificationDispatch.builder()
                        .user(user)
                        .notification(notification)
                        .type(type)
                        .scheduledAt(scheduledAt)
                        .build()
        );
    }

    @Transactional
    public List<Long> sendDueDispatches() {
        LocalDateTime now = timeService.nowKst();
        List<NotificationDispatch> dueDispatches = notificationDispatchRepository
                .findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(NotificationDispatchStatus.PENDING, now);

        for (NotificationDispatch dispatch : dueDispatches) {
            sendOne(dispatch, now);
        }

        return dueDispatches.stream().map(NotificationDispatch::getId).toList();
    }

    private void sendOne(NotificationDispatch dispatch, LocalDateTime now) {
        try {
            List<DeviceLog> deviceLogs = deviceLogRepository.findAllByUserIdAndFcmTokenIsNotNull(dispatch.getUser().getId());

            if (deviceLogs.isEmpty()) {
                markFailed(dispatch, now, "FCM 토큰이 존재하지 않습니다.");
                return;
            }

            String title = dispatch.getNotification() != null ? dispatch.getNotification().getTitle() : dispatch.getType().name();
            String body = dispatch.getNotification() != null ? dispatch.getNotification().getContent() : dispatch.getType().name();

            for (DeviceLog deviceLog : deviceLogs) {
                fcmPushService.send(deviceLog, title, body);
            }

            markSent(dispatch, now);
        } catch (Exception e) {
            markFailed(dispatch, now, e.getMessage());
        }
    }

    private void markSent(NotificationDispatch dispatch, LocalDateTime sentAt) {
        dispatch.setStatus(NotificationDispatchStatus.SENT);
        dispatch.setSentAt(sentAt);
        dispatch.setLastAttemptAt(sentAt);
        dispatch.setFailedReason(null);
        dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
    }

    private void markFailed(NotificationDispatch dispatch, LocalDateTime attemptedAt, String reason) {
        dispatch.setStatus(NotificationDispatchStatus.FAILED);
        dispatch.setLastAttemptAt(attemptedAt);
        dispatch.setFailedReason(reason);
        dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
    }
}
