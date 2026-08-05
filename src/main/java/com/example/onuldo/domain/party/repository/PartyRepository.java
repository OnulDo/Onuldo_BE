package com.example.onuldo.domain.party.repository;

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
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외).
     *
     * <p>진행률(progressRate)은 REC-02에 따라 상태별 분모가 달라(진행중=경과일수, 완료=전체진행일수)
     * 조회 결과의 원시값(시작일/종료일/총원/기간 내 PASS 수)을 서비스 계층에서 조합해 계산한다.
     * windowPassCount는 정산(POI-07)과 동일하게 예치일(시작일) 당일 인증은 수행일로 치지 않고 제외한다 —
     * 포함하면 분자(수행일 PASS 수)가 분모(경과 수행일수)보다 커져 진행률이 100%를 넘을 수 있다.
     * 반환 컬럼: [partyId, name, challengeTitle, goal, status, startDate, endDate, verificationDeadline,
     * totalMemberCount, verifiedMemberCount(오늘 인증 인원), windowPassCount(수행일 기간 내 팀 PASS 수), createdAt]
     *
     * <p>startDate/endDate는 파티원 참여 기록 전체의 MIN이 아니라 조회하는 본인(:userId)의 Participation을
     * 단일 원본으로 사용한다 (PartyService.generatePartyHomeItem과 동일 기준 — 코드리뷰 반영).
     */
    @Query("""
        SELECT
            p.id,
            p.name,
            c.name,
            c.explainContent,
            p.status,
            en.startDate,
            en.endDate,
            c.timeEnd,
            (SELECT COUNT(pm2) FROM PartyMember pm2 WHERE pm2.party.id = p.id),
            (SELECT COUNT(DISTINCT v.participation.user.id) FROM Verification v
                WHERE v.participation.party.id = p.id
                AND v.verificationDate = :today
                AND v.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS),
            (SELECT COUNT(v2) FROM Verification v2
                WHERE v2.participation.party.id = p.id
                AND v2.verificationDate > en.startDate
                AND v2.verificationDate <= :today
                AND v2.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS),
            p.createdAt
        FROM Party p
        JOIN PartyMember pm ON pm.party.id = p.id
        JOIN PartyChallenge pc ON pc.party.id = p.id
        JOIN pc.challenge c
        LEFT JOIN Participation en ON en.party.id = p.id
            AND en.user.id = :userId
            AND en.participationType = com.example.onuldo.domain.challenge.enums.ParticipationType.PARTY
        WHERE pm.user.id = :userId
        AND p.status NOT IN (com.example.onuldo.domain.party.enums.PartyStatus.WAITING, com.example.onuldo.domain.party.enums.PartyStatus.DISSOLVED)
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