package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.ChallengePot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChallengePotRepository extends JpaRepository<ChallengePot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM ChallengePot cp WHERE cp.id = :id")
    Optional<ChallengePot> findByIdForUpdate(@Param("id") Long id);
}
