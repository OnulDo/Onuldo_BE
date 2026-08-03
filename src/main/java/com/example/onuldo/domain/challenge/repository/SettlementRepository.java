package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByParticipation_IdInOrderByIdDesc(Collection<Long> participationIds);

    boolean existsByParticipation_Id(Long participationId);

    // POI-08: 파티 단위 1회 정산 멱등성 보장 — 파티 참여 중 하나라도 정산되어 있으면 재정산하지 않는다.
    boolean existsByParticipation_Party_Id(Long partyId);
}
