package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    List<NotificationDispatch> findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
            NotificationDispatchStatus status,
            LocalDateTime scheduledAt
    );

    boolean existsByNotification_Id(Long notificationId);

    Optional<NotificationDispatch> findFirstByNotification_IdOrderByIdDesc(Long notificationId);
}
