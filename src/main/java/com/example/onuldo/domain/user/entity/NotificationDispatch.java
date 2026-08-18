package com.example.onuldo.domain.user.entity;

import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.global.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification_dispatch",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_dispatch_notification",
                        columnNames = {"notification_id"}
                ),
                @UniqueConstraint(
                        name = "uk_notification_dispatch_reference",
                        columnNames = {"user_id", "type", "ref_type", "ref_id"}
                )
        }
)
public class NotificationDispatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_dispatch_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "notification_id")
    @Setter
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private NotificationDispatchStatus status = NotificationDispatchStatus.PENDING;

    @Column(name = "scheduled_at", nullable = false)
    @Setter
    private LocalDateTime scheduledAt;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "sent_at")
    @Setter
    private LocalDateTime sentAt;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    @Setter
    private Integer attemptCount = 0;

    @Column(name = "last_attempt_at")
    @Setter
    private LocalDateTime lastAttemptAt;

    @Column(name = "locked_at")
    @Setter
    private LocalDateTime lockedAt;

    @Column(name = "failed_reason", length = 500)
    @Setter
    private String failedReason;
}
