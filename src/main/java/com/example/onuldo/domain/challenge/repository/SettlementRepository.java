package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByParticipation_IdInOrderByIdDesc(Collection<Long> participationIds);
}
