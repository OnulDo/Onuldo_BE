package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsByParticipation_Id(Long participationId);
}
