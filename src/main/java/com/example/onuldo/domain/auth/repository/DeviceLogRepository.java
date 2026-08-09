package com.example.onuldo.domain.auth.repository;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
