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

    Optional<Notification> findFirstByUser_IdAndTypeAndRefTypeAndRefIdOrderByIdDesc(
            Long userId,
            NotificationType type,
            String refType,
            Long refId
    );

    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.user.id = :userId
            AND n.createdAt >= :storedAfter
            AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Notification> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("storedAfter") LocalDateTime storedAfter,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
