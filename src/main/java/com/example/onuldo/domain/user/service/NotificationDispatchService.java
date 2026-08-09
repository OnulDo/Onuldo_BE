package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import com.example.onuldo.domain.auth.repository.DeviceLogRepository;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.domain.user.repository.NotificationDispatchRepository;
import com.example.onuldo.domain.user.repository.NotificationSettingRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private static final int DISPATCH_BATCH_SIZE = 100;
    private static final int MAX_ATTEMPT_COUNT = 3;
    private static final Duration PROCESSING_LOCK_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration BASE_RETRY_BACKOFF = Duration.ofMinutes(1);
    private static final LocalTime QUIET_HOURS_START = LocalTime.of(23, 0);
    private static final LocalTime QUIET_HOURS_END = LocalTime.of(5, 0);

    private final NotificationDispatchRepository notificationDispatchRepository;
    private final UserRepository userRepository;
    private final NotificationCreateService notificationCreateService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final DeviceLogRepository deviceLogRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final FcmPushService fcmPushService;
    private final TimeService timeService;
    private final PlatformTransactionManager transactionManager;

    // 발송 시점에 알림함 기록을 만들 수 있도록 푸시 발송 대기열에 예약 정보만 등록하는 메서드
    public void enqueue(NotificationDispatchCommand command) {
        Notification notification = createReferenceNotification(command);
        try {
            executeWithoutResultInNewTransaction(status -> enqueueInTransaction(command, notification));
        } catch (DataIntegrityViolationException e) {
            if (notification == null) {
                throw e;
            }
            if (notificationDispatchRepository.findByNotification_Id(notification.getId()).isEmpty()) {
                throw e;
            }
        }
    }

    private Notification createReferenceNotification(NotificationDispatchCommand command) {
        if (command.getRefType() == null || command.getRefId() == null) {
            return null;
        }

        return notificationCreateService.createIfAbsent(
                command.getUserId(),
                command.getType(),
                command.getTitle(),
                command.getContent(),
                command.getRefType(),
                command.getRefId()
        );
    }

    private void enqueueInTransaction(NotificationDispatchCommand command, Notification notification) {
        if (notification != null && notificationDispatchRepository.findByNotification_Id(notification.getId()).isPresent()) {
            return;
        }

        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        notificationDispatchRepository.save(
                NotificationDispatch.builder()
                        .user(user)
                        .notification(notification)
                        .type(command.getType())
                        .scheduledAt(command.getScheduledAt())
                        .title(command.getTitle())
                        .content(command.getContent())
                        .refType(command.getRefType())
                        .refId(command.getRefId())
                        .build()
        );
    }

    // 발송 시간이 지난 PENDING 알림들을 야간 발송 제한을 적용해 FCM으로 전송하는 메서드
    public List<Long> sendDueDispatches() {
        LocalDateTime now = timeService.nowKst();
        LocalDateTime staleLockedAt = now.minus(PROCESSING_LOCK_TIMEOUT);
        List<Long> dueDispatchIds = executeInNewTransaction(status -> notificationDispatchRepository
                .findClaimableIdsOrderByScheduledAtAscIdAsc(
                        NotificationDispatchStatus.PENDING,
                        NotificationDispatchStatus.PROCESSING,
                        now,
                        staleLockedAt,
                        MAX_ATTEMPT_COUNT,
                        PageRequest.of(0, DISPATCH_BATCH_SIZE)
                ));

        boolean quietHours = isQuietHours(now.toLocalTime());
        return dueDispatchIds.stream()
                .map(dispatchId -> claimDispatch(dispatchId, now, staleLockedAt, quietHours))
                .flatMap(Optional::stream)
                .map(dispatch -> sendOne(dispatch, now))
                .toList();
    }

    private Optional<NotificationDispatch> claimDispatch(
            Long dispatchId,
            LocalDateTime now,
            LocalDateTime staleLockedAt,
            boolean quietHours
    ) {
        return executeInNewTransaction(status -> {
            Optional<NotificationDispatch> dispatchOptional =
                    notificationDispatchRepository.findWithUserAndNotificationById(dispatchId);
            if (dispatchOptional.isEmpty()) {
                return Optional.empty();
            }

            NotificationDispatch dispatch = dispatchOptional.get();
            boolean claimablePending = dispatch.getStatus() == NotificationDispatchStatus.PENDING;
            boolean claimableStaleProcessing = dispatch.getStatus() == NotificationDispatchStatus.PROCESSING
                    && dispatch.getLockedAt() != null
                    && !dispatch.getLockedAt().isAfter(staleLockedAt);
            if ((!claimablePending && !claimableStaleProcessing)
                    || dispatch.getAttemptCount() >= MAX_ATTEMPT_COUNT
                    || (quietHours && !isQuietHoursException(dispatch))) {
                return Optional.empty();
            }

            int updated = notificationDispatchRepository.claimIfAvailable(
                    dispatchId,
                    NotificationDispatchStatus.PENDING,
                    NotificationDispatchStatus.PROCESSING,
                    now,
                    staleLockedAt,
                    MAX_ATTEMPT_COUNT
            );
            if (updated != 1) {
                return Optional.empty();
            }

            return notificationDispatchRepository.findWithUserAndNotificationById(dispatchId);
        });
    }

    private Long sendOne(NotificationDispatch dispatch, LocalDateTime now) {
        try {
            Notification notification = createNotificationIfAbsent(dispatch);
            dispatch.setNotification(notification);

            if (!isPushEnabled(dispatch.getUser().getId(), dispatch.getType())) {
                finalizeDispatch(dispatch, () -> markCanceled(dispatch, now, "사용자 알림 설정이 꺼져 있습니다."));
                return dispatch.getId();
            }

            List<DeviceLog> deviceLogs = deviceLogRepository.findAllByUserIdAndFcmTokenIsNotBlank(dispatch.getUser().getId());

            if (deviceLogs.isEmpty()) {
                finalizeDispatch(dispatch, () -> markFailedOrRetry(dispatch, now, "FCM 토큰이 존재하지 않습니다."));
                return dispatch.getId();
            }

            int failureCount = 0;
            String lastFailureReason = null;
            Map<String, String> pushData = buildPushData(dispatch, notification);
            for (DeviceLog deviceLog : deviceLogs) {
                try {
                    fcmPushService.send(deviceLog, notification.getTitle(), notification.getContent(), pushData);
                } catch (Exception e) {
                    failureCount++;
                    lastFailureReason = e.getMessage();
                    log.warn("FCM 발송 실패. dispatchId={}, deviceLogId={}", dispatch.getId(), deviceLog.getId(), e);
                }
            }

            if (failureCount == deviceLogs.size()) {
                String reason = lastFailureReason == null ? "모든 FCM 토큰 발송에 실패했습니다." : lastFailureReason;
                finalizeDispatch(dispatch, () -> markFailedOrRetry(dispatch, now, reason));
            } else {
                finalizeDispatch(dispatch, () -> markSent(dispatch, now));
            }
        } catch (Exception e) {
            finalizeDispatch(dispatch, () -> markFailedOrRetry(dispatch, now, e.getMessage()));
        }

        return dispatch.getId();
    }

    private void finalizeDispatch(NotificationDispatch dispatch, Runnable transition) {
        executeWithoutResultInNewTransaction(status -> {
            transition.run();
            notificationDispatchRepository.save(dispatch);
        });
    }

    private <T> T executeInNewTransaction(TransactionCallback<T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(callback);
    }

    private void executeWithoutResultInNewTransaction(Consumer<TransactionStatus> callback) {
        executeInNewTransaction(status -> {
            callback.accept(status);
            return null;
        });
    }

    private void markSent(NotificationDispatch dispatch, LocalDateTime sentAt) {
        dispatch.setStatus(NotificationDispatchStatus.SENT);
        dispatch.setSentAt(sentAt);
        dispatch.setLastAttemptAt(sentAt);
        dispatch.setLockedAt(null);
        dispatch.setFailedReason(null);
        dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
    }

    private void markFailedOrRetry(NotificationDispatch dispatch, LocalDateTime attemptedAt, String reason) {
        int nextAttemptCount = dispatch.getAttemptCount() + 1;
        dispatch.setStatus(nextAttemptCount >= MAX_ATTEMPT_COUNT
                ? NotificationDispatchStatus.FAILED
                : NotificationDispatchStatus.PENDING);
        dispatch.setLastAttemptAt(attemptedAt);
        dispatch.setLockedAt(null);
        dispatch.setScheduledAt(attemptedAt.plus(resolveRetryBackoff(nextAttemptCount)));
        dispatch.setFailedReason(reason);
        dispatch.setAttemptCount(nextAttemptCount);
    }

    private void markCanceled(NotificationDispatch dispatch, LocalDateTime attemptedAt, String reason) {
        dispatch.setStatus(NotificationDispatchStatus.CANCELED);
        dispatch.setLastAttemptAt(attemptedAt);
        dispatch.setLockedAt(null);
        dispatch.setFailedReason(reason);
        dispatch.setAttemptCount(dispatch.getAttemptCount() + 1);
    }

    private Duration resolveRetryBackoff(int attemptCount) {
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        return BASE_RETRY_BACKOFF.multipliedBy(multiplier);
    }

    // 발송 처리 시점에 알림함용 Notification을 중복 없이 생성하는 메서드
    private Notification createNotificationIfAbsent(NotificationDispatch dispatch) {
        return Optional.ofNullable(dispatch.getNotification())
                .orElseGet(() -> notificationCreateService.createIfAbsent(
                                dispatch.getUser().getId(),
                                dispatch.getType(),
                                dispatch.getTitle(),
                                dispatch.getContent(),
                                dispatch.getRefType(),
                                dispatch.getRefId()
                        ));
    }

    // 사용자 알림 설정에 따라 푸시 발송 대기열 등록 여부를 판단하는 메서드
    private boolean isPushEnabled(Long userId, NotificationType type) {
        return notificationSettingRepository.findById(userId)
                .map(setting -> setting.isEnabled(type))
                .orElse(true);
    }

    private Map<String, String> buildPushData(NotificationDispatch dispatch, Notification notification) {
        Map<String, String> data = new LinkedHashMap<>();
        putIfNotNull(data, "notificationId", notification.getId());
        putIfNotNull(data, "notificationType", dispatch.getType());
        putIfNotNull(data, "refType", dispatch.getRefType());
        putIfNotNull(data, "refId", dispatch.getRefId());
        data.put("landing", resolveLanding(dispatch.getType()));
        data.put("fallbackLanding", "HOME");

        NotificationTarget target = resolveNotificationTarget(dispatch);
        putIfNotNull(data, "participationId", target.participationId());
        putIfNotNull(data, "challengeId", target.challengeId());
        putIfNotNull(data, "partyId", target.partyId());
        putIfNotNull(data, "verificationId", target.verificationId());
        return data;
    }

    private String resolveLanding(NotificationType type) {
        return switch (type) {
            case VERIFICATION_DEADLINE, CHALLENGE_START -> "HOME";
            case VERIFICATION_RESULT, CHALLENGE_END_REMINDER -> "CHALLENGE_DETAIL";
            case PARTY_MEMBER_VERIFIED -> "PARTY_FEED";
            case REFUND_COMPLETE -> "SOLO_RECORD_COMPLETED";
            case PARTY_SETTLEMENT_COMPLETE -> "PARTY_SETTLEMENT_RESULT";
        };
    }

    private NotificationTarget resolveNotificationTarget(NotificationDispatch dispatch) {
        Long verificationId = resolveVerificationId(dispatch.getRefType());
        if (verificationId != null) {
            Optional<NotificationTarget> target = findTargetByVerificationId(verificationId);
            if (target.isPresent()) {
                return withFallbackPartyId(target.get(), dispatch);
            }
        }

        Long participationId = resolveParticipationId(dispatch);
        if (participationId != null) {
            Optional<NotificationTarget> target = findTargetByParticipationId(participationId)
                    .map(found -> withVerificationId(found, verificationId));
            if (target.isPresent()) {
                return withFallbackPartyId(target.get(), dispatch);
            }
        }

        Long partyId = dispatch.getType() == NotificationType.PARTY_MEMBER_VERIFIED
                ? dispatch.getRefId()
                : null;
        return new NotificationTarget(participationId, null, partyId, verificationId);
    }

    private Optional<NotificationTarget> findTargetByVerificationId(Long verificationId) {
        return verificationRepository.findAllByIdInWithParticipation(List.of(verificationId))
                .stream()
                .findFirst()
                .map(this::toNotificationTarget);
    }

    private Optional<NotificationTarget> findTargetByParticipationId(Long participationId) {
        return participationRepository.findAllByIdInWithChallengeAndParty(List.of(participationId))
                .stream()
                .findFirst()
                .map(this::toNotificationTarget);
    }

    private NotificationTarget toNotificationTarget(Verification verification) {
        NotificationTarget target = toNotificationTarget(verification.getParticipation());
        return new NotificationTarget(
                target.participationId(),
                target.challengeId(),
                target.partyId(),
                verification.getId()
        );
    }

    private NotificationTarget toNotificationTarget(Participation participation) {
        Long partyId = participation.getParty() == null ? null : participation.getParty().getId();
        return new NotificationTarget(
                participation.getId(),
                participation.getChallenge().getId(),
                partyId,
                null
        );
    }

    private NotificationTarget withVerificationId(NotificationTarget target, Long verificationId) {
        if (target.verificationId() != null || verificationId == null) {
            return target;
        }
        return new NotificationTarget(
                target.participationId(),
                target.challengeId(),
                target.partyId(),
                verificationId
        );
    }

    private NotificationTarget withFallbackPartyId(NotificationTarget target, NotificationDispatch dispatch) {
        if (target.partyId() != null || dispatch.getType() != NotificationType.PARTY_MEMBER_VERIFIED) {
            return target;
        }
        return new NotificationTarget(
                target.participationId(),
                target.challengeId(),
                dispatch.getRefId(),
                target.verificationId()
        );
    }

    private Long resolveParticipationId(NotificationDispatch dispatch) {
        String refType = dispatch.getRefType();
        if (refType == null) {
            return null;
        }

        if (refType.startsWith("DEADLINE:")
                || refType.equals("CHALLENGE_START")
                || refType.startsWith("END_REMINDER:")
                || refType.startsWith("SETTLEMENT:")) {
            return dispatch.getRefId();
        }

        return null;
    }

    private Long resolveVerificationId(String refType) {
        Long parsedId = parseIdSuffix(refType, "PARTY_VERIFIED:");
        if (parsedId != null) {
            return parsedId;
        }

        parsedId = parseIdSuffix(refType, "MANUAL_PASS:");
        if (parsedId != null) {
            return parsedId;
        }

        return parseIdSuffix(refType, "MANUAL_REJECT:");
    }

    private Long parseIdSuffix(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return null;
        }

        try {
            return Long.parseLong(value.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void putIfNotNull(Map<String, String> data, String key, Object value) {
        if (value != null) {
            data.put(key, String.valueOf(value));
        }
    }

    // 현재 시간이 푸시 발송 제한 시간대인지 확인하는 메서드
    private boolean isQuietHours(LocalTime time) {
        return !time.isBefore(QUIET_HOURS_START) || time.isBefore(QUIET_HOURS_END);
    }

    // 발송 제한 시간대에도 즉시 보낼 수 있는 알림인지 확인하는 메서드
    private boolean isQuietHoursException(NotificationDispatch dispatch) {
        return dispatch.getType() == NotificationType.VERIFICATION_DEADLINE;
    }

    private record NotificationTarget(
            Long participationId,
            Long challengeId,
            Long partyId,
            Long verificationId
    ) {
    }
}
