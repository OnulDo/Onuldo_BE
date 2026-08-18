package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.Party;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
     * 나의 파티 목록 조회 원시값 (PAR-07: WAITING/DISSOLVED 제외).
     * 반환 컬럼: [partyId, name, challengeTitle, goal, status, startDate, endDate, verificationDeadline,
     * totalMemberCount, verifiedMemberCount(오늘 인증 인원), windowPassCount(수행일 기간 내 팀 PASS 수), challengeId]
     *
     * windowPassCount는 시작일 당일 인증도 수행일로 포함한다(당일 시작·당일 인증 정책과 동일 기준).
     * startDate/endDate는 파티원 전체의 MIN이 아니라 본인(:userId)의 Participation 기준(PartyService.generatePartyHomeItem과 동일).
     * 정렬(HOME-09)은 "오늘 나의 인증 상태" 같은 계산값 기준이라 DB가 아닌 서비스 계층에서 전체를 가져와 처리한다.
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
                AND v2.verificationDate >= en.startDate
                AND v2.verificationDate <= :today
                AND v2.review = com.example.onuldo.domain.challenge.enums.VerificationReviewStatus.PASS),
            c.id
        FROM Party p
        JOIN PartyMember pm ON pm.party.id = p.id
        JOIN PartyChallenge pc ON pc.party.id = p.id
        JOIN pc.challenge c
        LEFT JOIN Participation en ON en.party.id = p.id
            AND en.user.id = :userId
            AND en.participationType = com.example.onuldo.domain.challenge.enums.ParticipationType.PARTY
        WHERE pm.user.id = :userId
        AND p.status NOT IN (com.example.onuldo.domain.party.enums.PartyStatus.WAITING, com.example.onuldo.domain.party.enums.PartyStatus.DISSOLVED)
        """)
    List<Object[]> findMyPartiesExcludingWaiting(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
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