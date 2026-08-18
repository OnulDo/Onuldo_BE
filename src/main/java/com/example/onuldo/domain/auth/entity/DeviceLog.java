package com.example.onuldo.domain.auth.entity;

import com.example.onuldo.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import com.example.onuldo.global.common.time.TimeService;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "device_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_log_user_device",
                        columnNames = {"user_id", "device_id"}
                )
        }
)
public class DeviceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Builder.Default
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt = TimeService.nowKstStatic();

    public void update(String fcmToken, LocalDateTime lastSeenAt) {
        this.fcmToken = fcmToken;
        this.lastSeenAt = lastSeenAt;
    }
}
