package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    @Query("""
            SELECT nd.id
            FROM NotificationDispatch nd
            WHERE nd.status = :status
            AND nd.scheduledAt <= :scheduledAt
            ORDER BY nd.scheduledAt ASC, nd.id ASC
            """)
    List<Long> findIdsByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscIdAsc(
            @Param("status") NotificationDispatchStatus status,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationDispatch nd
            SET nd.status = :nextStatus,
                nd.lockedAt = :lockedAt
            WHERE nd.id = :id
            AND nd.status = :currentStatus
            """)
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("currentStatus") NotificationDispatchStatus currentStatus,
            @Param("nextStatus") NotificationDispatchStatus nextStatus,
            @Param("lockedAt") LocalDateTime lockedAt
    );

    Optional<NotificationDispatch> findByNotification_Id(Long notificationId);

    @EntityGraph(attributePaths = {"user", "notification"})
    Optional<NotificationDispatch> findWithUserAndNotificationById(Long id);
}
