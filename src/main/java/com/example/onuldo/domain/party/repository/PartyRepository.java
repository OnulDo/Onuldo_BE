package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.entity.Party;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    /**
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외)
     */
    //TODO-challengeTitle은 Challenge 엔티티 확인 후 PartyChallenge JOIN으로 실제 값 채워야 함. 현재는 빈 문자열.
    //TODO-endDate는 Party/Challenge 진행 기간 정보가 확정되면 실제 값으로 채워야 함. 현재는 CURRENT_DATE로 임시 채움.
    //TODO-verificationDeadline은 Challenge 인증 가능 시간 정보 확인 후 채워야 함. 현재는 CURRENT_TIME으로 임시 채움.
    //TODO-progressRate(진행률)는 PARTICIPATION/VERIFICATION 테이블 조인이 필요해서 우선 0으로 채워둠. 서브쿼리나 별도 조회로 채워야 함.
    //TODO-verifiedMemberCount(오늘 인증 인원)는 PARTICIPATION/VERIFICATION 테이블 조인이 필요해서 우선 0으로 채워둠. 서브쿼리나 별도 조회로 채워야 함.

    @Query("""
        SELECT new com.example.onuldo.domain.party.dto.response.PartyListResDto(
            p.id,
            p.name,
            '',
            p.status,
            LOCAL_DATE,
            LOCAL_TIME,
            0.0D,
            CAST(0 AS integer),
            CAST((SELECT COUNT(pm2) FROM PartyMember pm2 WHERE pm2.party.id = p.id) AS integer)
        ), p.createdAt
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
            Pageable pageable
    );
}