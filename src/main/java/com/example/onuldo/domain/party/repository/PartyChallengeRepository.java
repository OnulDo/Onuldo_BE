package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyChallengeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyChallengeRepository extends JpaRepository<PartyChallenge, PartyChallengeId> {

    Optional<PartyChallenge> findByParty_Id(Long partyId);
}
