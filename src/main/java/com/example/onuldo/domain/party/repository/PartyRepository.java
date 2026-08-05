package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.Party;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Party p WHERE p.id = :id")
    Optional<Party> findByIdForUpdate(@Param("id") Long id);

    // PAR-04 동시성 방지: 초대코드로 파티에 참여할 때 정원 체크~저장 사이의
    // 레이스 컨디션(정원 초과 참여)을 막기 위해 파티 행에 비관적 락을 건다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Party p WHERE p.inviteCode = :inviteCode")
    Optional<Party> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

    /**
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외)
     */
    // 코드리뷰 반영: endDate는 파티 시작 시 생성된 Participation.endDate를 단일 원본으로 사용
    // (PartyService.generatePartyHomeItem도 동일 기준으로 통일)
    @Query("""
        SELECT new com.example.onuldo.domain.party.dto.response.PartyListResDto(
            p.id,
            p.name,
            c.name,
            p.status,
            en.endDate,
            c.timeEnd,
            CAST(
                (SELECT COUNT(v)
                 FROM Verification v
                 WHERE v.participation.party.id = p.id
                 AND v.verificationDate = LOCAL_DATE
                 AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS)
                AS double
            )
            / (SELECT COUNT(pm2) FROM PartyMember pm2 WHERE pm2.party.id = p.id),
            CAST(
                (SELECT COUNT(v2)
                 FROM Verification v2
                 WHERE v2.participation.party.id = p.id
                 AND v2.verificationDate = LOCAL_DATE
                 AND v2.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS)
                AS integer
            ),
            CAST((SELECT COUNT(pm3) FROM PartyMember pm3 WHERE pm3.party.id = p.id) AS integer)
        ), p.createdAt
        FROM Party p
        JOIN PartyMember pm ON pm.party.id = p.id
        JOIN PartyChallenge pc ON pc.party.id = p.id
        JOIN Challenge c ON c.id = pc.challenge.id
        LEFT JOIN Participation en ON en.party.id = p.id
            AND en.user.id = :userId
            AND en.participationType = com.example.onuldo.domain.challenge.enums.ParticipationType.PARTY
        WHERE pm.user.id = :userId
        AND p.status <> com.example.onuldo.domain.party.enums.PartyStatus.WAITING
        AND (
            :lastCreatedAt IS NULL
            OR p.createdAt < :lastCreatedAt
            OR (p.createdAt = :lastCreatedAt AND p.id < :lastId)
        )
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Object[]> findMyPartiesExcludingWaiting(
            @Param("userId") Long userId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    // HOME-07: 홈 화면 "함께하는 파티" 섹션용 - 진행 중인 파티만 조회
    @Query("""
            SELECT p
            FROM Party p
            JOIN PartyMember pm ON pm.party.id = p.id
            WHERE pm.user.id = :userId
            AND p.status = com.example.onuldo.domain.party.enums.PartyStatus.ONGOING
            """)
    List<Party> findOngoingPartiesByUserId(@Param("userId") Long userId);
}