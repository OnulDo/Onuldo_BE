package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.user.dto.response.NotificationListItemResDto;
import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.repository.NotificationRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int RETENTION_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final TimeService timeService;

    // 로그인한 유저의 30일 이내 알림을 최신순 커서 페이징으로 조회하는 메서드
    public CursorPageResponse<NotificationListItemResDto> getNotifications(
            Long userId,
            String cursor,
            int size
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);
        Long lastId = CursorKeyCodec.isBlank(cursor) ? null : CursorKeyCodec.decodeAsLongs(cursor, 1)[0];
        LocalDateTime storedAfter = timeService.nowKst().minusDays(RETENTION_DAYS);

        List<Notification> notifications = notificationRepository.findByUserIdWithCursor(
                userId,
                storedAfter,
                lastId,
                CursorPageable.of(resolvedSize)
        );

        return CursorPageResponse.of(
                notifications,
                resolvedSize,
                this::toNotificationListItemResDto,
                notification -> CursorKeyCodec.encode(notification.getId())
        );
    }

    private NotificationListItemResDto toNotificationListItemResDto(Notification notification) {
        NotificationTarget target = resolveTarget(notification);

        return NotificationListItemResDto.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .challengeId(target.challengeId())
                .partyId(target.partyId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .timeAgo(formatTimeAgo(notification.getCreatedAt()))
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // 알림의 참조 정보를 기반으로 이동에 필요한 챌린지 ID와 파티 ID를 찾는 메서드
    private NotificationTarget resolveTarget(Notification notification) {
        Long verificationId = resolveVerificationId(notification);
        if (verificationId != null) {
            return verificationRepository.findById(verificationId)
                    .map(Verification::getParticipation)
                    .map(this::toNotificationTarget)
                    .orElse(NotificationTarget.empty());
        }

        Long participationId = resolveParticipationId(notification);
        if (participationId != null) {
            return participationRepository.findById(participationId)
                    .map(this::toNotificationTarget)
                    .orElse(NotificationTarget.empty());
        }

        if (notification.getRefType() != null && notification.getRefType().startsWith("PARTY_DAILY:")) {
            return new NotificationTarget(null, notification.getRefId());
        }

        if (notification.getRefType() != null && notification.getRefType().startsWith("PARTY_VERIFIED:")) {
            return new NotificationTarget(null, notification.getRefId());
        }

        return NotificationTarget.empty();
    }

    private NotificationTarget toNotificationTarget(Participation participation) {
        Long partyId = participation.getParty() == null ? null : participation.getParty().getId();
        return new NotificationTarget(participation.getChallenge().getId(), partyId);
    }

    private Long resolveParticipationId(Notification notification) {
        if (notification.getRefType() == null) {
            return null;
        }

        if (notification.getRefType().startsWith("DEADLINE:")
                || notification.getRefType().equals("CHALLENGE_START")
                || notification.getRefType().startsWith("END_REMINDER:")) {
            return notification.getRefId();
        }

        return parseIdSuffix(notification.getRefType(), "SETTLEMENT:");
    }

    private Long resolveVerificationId(Notification notification) {
        Long parsedId = parseIdSuffix(notification.getRefType(), "PARTY_VERIFIED:");
        if (parsedId != null) {
            return parsedId;
        }

        parsedId = parseIdSuffix(notification.getRefType(), "MANUAL_PASS:");
        if (parsedId != null) {
            return parsedId;
        }

        return parseIdSuffix(notification.getRefType(), "MANUAL_REJECT:");
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

    // 알림 생성 시각을 방금/분 전/시간 전/일 전 형식으로 변환하는 메서드
    private String formatTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, timeService.nowKst());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) {
            return "방금";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "시간 전";
        }

        return duration.toDays() + "일 전";
    }
    private record NotificationTarget(Long challengeId, Long partyId) {
        private static NotificationTarget empty() {
            return new NotificationTarget(null, null);
        }
    }
}
