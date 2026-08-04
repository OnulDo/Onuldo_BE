package com.example.onuldo.domain.auth.repository;

import com.example.onuldo.domain.auth.entity.DeviceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceLogRepository extends JpaRepository<DeviceLog, Long> {

    List<DeviceLog> findAllByUserIdAndFcmTokenIsNotNull(Long userId);

    List<DeviceLog> findAllByFcmTokenIsNotNull();
}
