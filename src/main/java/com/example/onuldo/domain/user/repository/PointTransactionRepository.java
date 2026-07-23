package com.example.onuldo.domain.user.repository;

import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    @Query("SELECT COALESCE(SUM(pt.amount), 0) " +
            "FROM PointTransaction pt " +
            "WHERE pt.user.id = :userId AND pt.type = :type")
    Long sumAmountByUserIdAndType(
            @Param("userId") Long userId, @Param("type") PointTransactionType type
    );
}
