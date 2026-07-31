package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);
    @Query("""
        SELECT COALESCE(SUM(p.depositAmount), 0)
        FROM Participation p
        WHERE p.user.id = :userId
        AND p.status = :status
    """)
    Long sumDepositAmountByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(p.depositAmount), 0)
        FROM Participation p
        WHERE p.user.id = :userId
        AND p.status <> :status
    """)
    Long sumDepositAmountByUserIdAndStatusNot(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status
    );


    @Query("""
        SELECT p FROM Participation p
        WHERE p.user.id = :userId
        AND (:lastId IS NULL OR p.id < :lastId)
        ORDER BY p.id DESC
    """)
    List<Participation> findAllByUser_IdOrderByIdDesc(
            @Param("userId") Long userId,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Participation p
        WHERE p.user.id = :userId
        AND p.status = :status
        AND (:lastId IS NULL OR p.id < :lastId)
        ORDER BY p.id DESC
    """)
    List<Participation> findAllByUser_IdAndStatusOrderByIdDesc(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
    List<Participation> findAllByUser_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
            Long userId,
            ParticipationStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Participation> findTopByUser_IdAndChallenge_IdAndStatusOrderByIdDesc(
            Long userId,
            Long challengeId,
            ParticipationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Participation p WHERE p.id = :id")
    Optional<Participation> findByIdForUpdate(@Param("id") Long id);

    List<Participation> findAllByIdIn(Collection<Long> ids);
}
