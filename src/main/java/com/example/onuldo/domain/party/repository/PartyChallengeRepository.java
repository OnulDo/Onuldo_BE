package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyChallengeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyChallengeRepository extends JpaRepository<PartyChallenge, PartyChallengeId> {
}
