package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findFirstByUser_IdAndTypeAndRefTypeAndRefIdOrderByIdDesc(
            Long userId,
            NotificationType type,
            String refType,
            Long refId
    );
}
