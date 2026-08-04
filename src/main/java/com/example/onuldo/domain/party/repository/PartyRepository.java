package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.dto.response.PartyListResDto;
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

    /**
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외)
     */
    //TODO-endDate는 Party/Challenge 진행 기간 정보가 확정되면 실제 값으로 채워야 함. 현재는 CURRENT_DATE로 임시 채움.
    // (WAITING 파티도 이 API로 조회될 가능성이 있어 startTriggeredAt이 null일 수 있는 케이스 정책 확인 필요 - 팀원 확인 예정)
    @Query("""
        SELECT new com.example.onuldo.domain.party.dto.response.PartyListResDto(
            p.id,
            p.name,
            c.name,
            p.status,
            LOCAL_DATE,
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