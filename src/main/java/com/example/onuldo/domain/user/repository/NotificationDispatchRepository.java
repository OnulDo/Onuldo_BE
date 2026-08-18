package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.NotificationDispatch;
import com.example.onuldo.domain.user.enums.NotificationDispatchStatus;
import com.example.onuldo.domain.user.enums.NotificationType;
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
            WHERE nd.attemptCount < :maxAttemptCount
            AND (
                (nd.status = :pendingStatus AND nd.scheduledAt <= :scheduledAt)
                OR (nd.status = :processingStatus AND nd.lockedAt <= :staleLockedAt)
            )
            ORDER BY nd.scheduledAt ASC, nd.id ASC
            """)
    List<Long> findClaimableIdsOrderByScheduledAtAscIdAsc(
            @Param("pendingStatus") NotificationDispatchStatus pendingStatus,
            @Param("processingStatus") NotificationDispatchStatus processingStatus,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("staleLockedAt") LocalDateTime staleLockedAt,
            @Param("maxAttemptCount") int maxAttemptCount,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationDispatch nd
            SET nd.status = :processingStatus,
                nd.lockedAt = :lockedAt
            WHERE nd.id = :id
            AND nd.attemptCount < :maxAttemptCount
            AND (
                nd.status = :pendingStatus
                OR (nd.status = :processingStatus AND nd.lockedAt <= :staleLockedAt)
            )
            """)
    int claimIfAvailable(
            @Param("id") Long id,
            @Param("pendingStatus") NotificationDispatchStatus pendingStatus,
            @Param("processingStatus") NotificationDispatchStatus processingStatus,
            @Param("lockedAt") LocalDateTime lockedAt,
            @Param("staleLockedAt") LocalDateTime staleLockedAt,
            @Param("maxAttemptCount") int maxAttemptCount
    );

    Optional<NotificationDispatch> findByUser_IdAndTypeAndRefTypeAndRefId(
            Long userId,
            NotificationType type,
            String refType,
            Long refId
    );

    @EntityGraph(attributePaths = {"user", "notification"})
    Optional<NotificationDispatch> findWithUserAndNotificationById(Long id);
}
