package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
            SELECT s
            FROM Settlement s
            JOIN FETCH s.participation p
            JOIN FETCH p.user u
            WHERE p.party.id = :partyId
            ORDER BY p.id ASC
            """)
    List<Settlement> findByPartyId(@Param("partyId") Long partyId);

    List<Settlement> findAllByParticipation_IdInOrderByIdDesc(Collection<Long> participationIds);

    boolean existsByParticipation_Id(Long participationId);

    // POI-08: 파티 단위 1회 정산 멱등성 보장 — 파티 참여 중 하나라도 정산되어 있으면 재정산하지 않는다.
    boolean existsByParticipation_Party_Id(Long partyId);

    // 홈 상단 "정산이 완료됐어요!" 배너: 아직 확인하지 않은 파티 정산 전체 (확인 시 Settlement.confirm()으로 제외됨)
    // 솔로(PERSONAL) 정산은 party가 없어 배너 대상이 아니므로 PARTY 타입만 조회한다.
    @Query("""
            SELECT s
            FROM Settlement s
            JOIN FETCH s.participation p
            JOIN FETCH p.party
            WHERE p.user.id = :userId
            AND p.participationType = com.example.onuldo.domain.challenge.enums.ParticipationType.PARTY
            AND s.status = com.example.onuldo.domain.challenge.enums.SettlementStatus.COMPLETED
            AND s.confirmed = false
            ORDER BY s.processedAt DESC
            """)
    List<Settlement> findUnconfirmedPartySettlementsByUserId(@Param("userId") Long userId);
}
