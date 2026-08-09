package com.example.onuldo.domain.challenge.repository;

import com.example.onuldo.domain.challenge.entity.PotTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotTransactionRepository extends JpaRepository<PotTransaction, Long> {
}
