package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    /**
     * 나의 파티 목록 조회 (PAR-07: WAITING 상태 파티는 목록에서 제외)
     *
     * TODO: progressRate(진행률), verifiedToday(오늘 인증 인원)는
     * PARTICIPATION / VERIFICATION 테이블 조인이 필요해서 우선 0으로 채워둠.
     * 해당 API 붙으면 서브쿼리나 별도 조회로 채워야 함.
     */
    @Query("""
            SELECT new com.example.onuldo.domain.party.dto.response.PartyListResDto(
                p.id,
                p.name,
                p.status,
                CAST(0 AS integer),
                0.0D,
                CAST(0 AS integer),
                CAST((SELECT COUNT(pm2) FROM PartyMember pm2 WHERE pm2.party.id = p.id) AS integer)
            )
            FROM Party p
            JOIN PartyMember pm ON pm.party.id = p.id
            WHERE pm.user.id = :userId
            AND p.status <> com.example.onuldo.domain.party.enums.PartyStatus.WAITING
            ORDER BY p.createdAt DESC
            """)
    List<PartyListResDto> findMyPartiesExcludingWaiting(@Param("userId") Long userId);
}