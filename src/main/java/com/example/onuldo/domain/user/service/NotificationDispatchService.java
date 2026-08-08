package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import com.example.onuldo.domain.auth.repository.DeviceLogRepository;
import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.repository.NotificationDispatchRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDispatchService {

    private static final LocalTime QUIET_HOURS_START = LocalTime.of(23, 0);
    private static final LocalTime QUIET_HOURS_END = LocalTime.of(5, 0);

    private final NotificationDispatchRepository notificationDispatchRepository;
    private final UserRepository userRepository;
    private final NotificationCreateService notificationCreateService;
    private final DeviceLogRepository deviceLogRepository;
    private final FcmPushService fcmPushService;
    private final TimeService timeService;

    // 알림함 기록을 만들고 푸시 발송 대기열에 NotificationDispatch를 등록하는 메서드
    @Transactional
    public NotificationDispatch enqueue(NotificationDispatchCommand command) {
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        Notification notification = notificationCreateService.createIfAbsent(
                command.getUserId(),
                command.getType(),
                command.getTitle(),
                command.getContent(),
                command.getRefType(),
                command.getRefId()
        );
        if (notificationDispatchRepository.existsByNotification_Id(notification.getId())) {
            return notificationDispatchRepository.findFirstByNotification_IdOrderByIdDesc(notification.getId())
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR));
        }

        return notificationDispatchRepository.save(
                NotificationDispatch.builder()
                        .user(user)
                        .notification(notification)
                        .type(command.getType())
                        .scheduledAt(command.getScheduledAt())
                        .build()
        );
    }

    // 발송 시간이 지난 PENDING 알림들을 야간 발송 제한을 적용해 FCM으로 전송하는 메서드
    @Transactional
    public List<Long> sendDueDispatches() {
        LocalDateTime now = timeService.nowKst();
        List<NotificationDispatch> dueDispatches = notificationDispatchRepository
                .findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(NotificationDispatchStatus.PENDING, now);

        // 조용 시간 반영 (23:00 ~ 05:00 푸시 알람 보류. 05:00시에 일괄 발송
        boolean quietHours = isQuietHours(now.toLocalTime());
        for (NotificationDispatch dispatch : dueDispatches) {
            if (quietHours && !isQuietHoursException(dispatch)) {
                continue;
            }

            sendOne(dispatch, now);
        }

        return dueDispatches.stream()
                .filter(dispatch -> !quietHours || isQuietHoursException(dispatch))
                .map(NotificationDispatch::getId)
                .toList();
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

    // 현재 시간이 푸시 발송 제한 시간대인지 확인하는 메서드
    private boolean isQuietHours(LocalTime time) {
        return !time.isBefore(QUIET_HOURS_START) || time.isBefore(QUIET_HOURS_END);
    }

    // 발송 제한 시간대에도 즉시 보낼 수 있는 알림인지 확인하는 메서드
    private boolean isQuietHoursException(NotificationDispatch dispatch) {
        return dispatch.getType() == NotificationType.VERIFICATION_DEADLINE;
    }
}
