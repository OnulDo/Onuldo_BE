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
}