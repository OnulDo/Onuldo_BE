package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    boolean existsByUser_IdAndStatus(Long userId, ParticipationStatus status);

    boolean existsByUser_IdAndChallenge_IdAndStatus(Long userId, Long challengeId, ParticipationStatus status);

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
            JOIN FETCH p.user
            JOIN FETCH p.challenge
            LEFT JOIN FETCH p.party
            WHERE p.status = :status
            AND p.startDate <= :date
            AND p.endDate >= :date
            """)
    List<Participation> findAllActiveOnDate(
            @Param("status") ParticipationStatus status,
            @Param("date") LocalDate date
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

    // 정산 시 유저 행 락(payoutPoint)을 user.id 오름차순으로 고정 — 서로 다른 파티 정산이 동시에 실행되고
    // 같은 유저가 두 파티에 속해있을 때, 락 획득 순서가 갈려 데드락이 나는 것을 방지한다.
    List<Participation> findAllByParty_IdAndStatusOrderByUser_IdAsc(Long partyId, ParticipationStatus status);

    // POI-08: 마지막 수행일의 인증 마감(챌린지 timeEnd)이 지난, 아직 미정산 파티의 partyId 목록.
    // 기존 실패 스케줄러는 마지막 날 PASS 한 파티원을 제외해 전원 성공 파티를 놓치므로, 파티 전용 스윕이 필요하다.
    @Query("""
        SELECT DISTINCT p.party.id
        FROM Participation p
        JOIN p.challenge c
        WHERE p.participationType = com.example.onuldo.domain.challenge.enums.ParticipationType.PARTY
          AND p.status = :status
          AND (
                p.endDate < :today
                OR (p.endDate = :today AND (c.timeEnd IS NULL OR c.timeEnd <= :currentTime))
          )
          AND NOT EXISTS (
                SELECT 1
                FROM Settlement s
                WHERE s.participation.party.id = p.party.id
          )
    """)
    List<Long> findPartySettlementTargetPartyIds(
            @Param("status") ParticipationStatus status,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime
    );

    List<Participation> findAllByIdIn(Collection<Long> ids);

    @Query("""
            SELECT p
            FROM Participation p
            JOIN FETCH p.user
            JOIN FETCH p.challenge
            WHERE p.party.id = :partyId
            AND p.status = :status
            """)
    List<Participation> findAllByPartyIdAndStatusWithUserAndChallenge(
            @Param("partyId") Long partyId,
            @Param("status") ParticipationStatus status
    );

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

    // HOME-07 코드리뷰 반영
    Optional<Participation> findByParty_IdAndUser_IdAndParticipationType(
            Long partyId,
            Long userId,
            ParticipationType participationType
    );
}
