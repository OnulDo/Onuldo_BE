package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    List<NotificationDispatch> findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
            NotificationDispatchStatus status,
            LocalDateTime scheduledAt
    );

    boolean existsByUser_IdAndTypeAndRefTypeAndRefId(
            Long userId,
            NotificationType type,
            String refType,
            Long refId
    );
}
