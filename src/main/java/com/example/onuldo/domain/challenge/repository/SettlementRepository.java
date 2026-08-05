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

    // 홈 상단 "정산이 완료됐어요!" 배너: 아직 확인하지 않은 정산 전체 (확인 시 Settlement.confirm()으로 제외됨)
    List<Settlement> findAllByParticipation_User_IdAndConfirmedFalseOrderByProcessedAtDesc(Long userId);
}
