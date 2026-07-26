package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    boolean existsByUser_IdAndDescription(Long userId, String description);

    @Query("SELECT COALESCE(SUM(pt.amount), 0) " +
            "FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.type = :type")
    Long sumAmountByUserIdAndType(
            @Param("userId") Long userId, @Param("type") PointTransactionType type
    );

    @Query("""
        SELECT pt FROM PointTransaction pt
        WHERE pt.user.id = :userId
        AND (:type IS NULL OR pt.type = :type)
        AND (:cursor IS NULL OR pt.id < :cursor)
        ORDER BY pt.id DESC
    """)
    List<PointTransaction> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("type") PointTransactionType type,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(-pt.adjustmentAmount), 0)
        FROM PointTransaction pt
        WHERE pt.user.id = :userId
        AND pt.type = com.example.onuldo.domain.user.enums.PointTransactionType.REFUND
        AND pt.adjustmentAmount < 0
    """)
    Long sumPenaltyAdjustmentByUserId(@Param("userId") Long userId);
}
