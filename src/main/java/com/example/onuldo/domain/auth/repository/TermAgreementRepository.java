package com.example.onuldo.domain.auth.repository;

import com.example.onuldo.domain.auth.entity.TermAgreement;
import com.example.onuldo.domain.auth.entity.TermAgreementId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermAgreementRepository extends JpaRepository<TermAgreement, TermAgreementId> {
}
