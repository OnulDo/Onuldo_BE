package com.example.onuldo.domain.auth.repository;

import com.example.onuldo.domain.auth.entity.Term;
import com.example.onuldo.domain.auth.enums.TermType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Integer> {

    Optional<Term> findByType(TermType type);
}
