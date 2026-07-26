package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
}
