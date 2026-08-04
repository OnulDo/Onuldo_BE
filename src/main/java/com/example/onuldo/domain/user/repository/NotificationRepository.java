package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
