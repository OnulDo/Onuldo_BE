package com.example.onuldo.domain.auth.repository;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceLogRepository extends JpaRepository<DeviceLog, Long> {

    @Query("""
            SELECT dl
            FROM DeviceLog dl
            WHERE dl.user.id = :userId
            AND dl.fcmToken IS NOT NULL
            AND TRIM(dl.fcmToken) <> ''
            """)
    List<DeviceLog> findAllByUserIdAndFcmTokenIsNotBlank(@Param("userId") Long userId);

    @Query("""
            SELECT dl
            FROM DeviceLog dl
            WHERE dl.fcmToken IS NOT NULL
            AND TRIM(dl.fcmToken) <> ''
            """)
    List<DeviceLog> findAllByFcmTokenIsNotBlank();

    Optional<DeviceLog> findByUser_IdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query(value = """
            INSERT INTO device_log (user_id, device_id, fcm_token, last_seen_at)
            VALUES (:userId, :deviceId, :fcmToken, :lastSeenAt)
            ON DUPLICATE KEY UPDATE
                fcm_token = VALUES(fcm_token),
                last_seen_at = VALUES(last_seen_at)
            """, nativeQuery = true)
    void upsert(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("fcmToken") String fcmToken,
            @Param("lastSeenAt") LocalDateTime lastSeenAt
    );
}
