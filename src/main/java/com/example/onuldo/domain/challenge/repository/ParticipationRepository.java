package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    boolean existsByUser_IdAndStatus(Long userId, ParticipationStatus status);

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

    @Query("""
            SELECT p
            FROM Participation p
            JOIN FETCH p.challenge
            WHERE p.user.id = :userId
            AND p.status = :status
            ORDER BY p.id DESC
            """)
    List<Participation> findAllWithChallengeByUserIdAndStatusOrderByIdDesc(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status
    );

    @Query("""
            SELECT p
            FROM Participation p
            JOIN FETCH p.challenge
            WHERE p.user.id = :userId
            AND p.status IN :statuses
            ORDER BY p.endDate DESC, p.id DESC
            """)
    List<Participation> findAllWithChallengeByUserIdAndStatusInOrderByEndDateDesc(
            @Param("userId") Long userId,
            @Param("statuses") Collection<ParticipationStatus> statuses
    );
    Optional<Participation> findTopByUser_IdAndChallenge_IdAndStatusOrderByIdDesc(
            Long userId,
            Long challengeId,
            ParticipationStatus status
    );

    @Query("""
        SELECT p
        FROM Participation p
        JOIN FETCH p.challenge c
        WHERE p.status = :status
          AND p.endDate <= :endDate
          AND c.timeEnd IS NOT NULL
          AND c.timeEnd <= :currentTime
          AND NOT EXISTS (
                SELECT 1
                FROM Verification v
                WHERE v.participation = p
                  AND v.verificationDate = :endDate
                  AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS
          )
          AND NOT EXISTS (
                SELECT 1
                FROM Settlement s
                WHERE s.participation = p
          )
    """)
    List<Participation> findFailedSettlementTargets(
            @Param("status") ParticipationStatus status,
            @Param("endDate") LocalDate endDate,
            @Param("currentTime") LocalTime currentTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Participation p WHERE p.id = :id")
    Optional<Participation> findByIdForUpdate(@Param("id") Long id);

    List<Participation> findAllByIdIn(Collection<Long> ids);

    @Query("""
        SELECT new com.example.onuldo.domain.challenge.repository.PartyCountProjection(p.party.id, COUNT(DISTINCT p.user.id))
        FROM Participation p
        WHERE p.party.id IN :partyIds
        AND p.status = :status
        GROUP BY p.party.id
    """)
    List<PartyCountProjection> findParticipationCountsByPartyIdInAndStatus(
            @Param("partyIds") Collection<Long> partyIds,
            @Param("status") ParticipationStatus status
    );
}
