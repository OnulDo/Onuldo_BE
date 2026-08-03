package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.entity.Party;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외).
     *
     * <p>진행률(progressRate)은 REC-02에 따라 상태별 분모가 달라(진행중=경과일수, 완료=전체진행일수)
     * 조회 결과의 원시값(시작일/종료일/총원/기간 내 PASS 수)을 서비스 계층에서 조합해 계산한다.
     * 반환 컬럼: [partyId, name, challengeTitle, status, startDate, endDate, verificationDeadline,
     * totalMemberCount, verifiedMemberCount(오늘 인증 인원), windowPassCount(기간 내 팀 PASS 수), createdAt]
     */
    @Query("""
        SELECT
            p.id,
            p.name,
            (SELECT c.name FROM PartyChallenge pc JOIN pc.challenge c WHERE pc.party.id = p.id),
            p.status,
            (SELECT MIN(pt.startDate) FROM Participation pt WHERE pt.party.id = p.id),
            (SELECT MIN(pt.endDate) FROM Participation pt WHERE pt.party.id = p.id),
            (SELECT c.timeEnd FROM PartyChallenge pc JOIN pc.challenge c WHERE pc.party.id = p.id),
            (SELECT COUNT(pm2) FROM PartyMember pm2 WHERE pm2.party.id = p.id),
            (SELECT COUNT(DISTINCT v.participation.user.id) FROM Verification v
                WHERE v.participation.party.id = p.id
                AND v.verificationDate = :today
                AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS),
            (SELECT COUNT(v2) FROM Verification v2
                WHERE v2.participation.party.id = p.id
                AND v2.verificationDate <= :today
                AND v2.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS),
            p.createdAt
        FROM Party p
        JOIN PartyMember pm ON pm.party.id = p.id
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
            @Param("today") LocalDate today,
            Pageable pageable
    );
}