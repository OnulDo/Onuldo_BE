package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.Notification;
import com.example.onuldo.domain.user.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByUser_IdAndTypeAndRefTypeAndRefId(
            Long userId,
            NotificationType type,
            String refType,
            Long refId
    );

    @Query("""
            SELECT n
            FROM Notification n
            LEFT JOIN NotificationDispatch nd ON nd.notification = n
            WHERE n.user.id = :userId
            AND n.createdAt >= :storedAfter
            AND (nd.id IS NULL OR nd.lastAttemptAt IS NOT NULL)
            AND (
                :cursorCreatedAt IS NULL
                OR n.createdAt < :cursorCreatedAt
                OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)
            )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("storedAfter") LocalDateTime storedAfter,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
