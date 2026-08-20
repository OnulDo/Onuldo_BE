package com.example.onuldo.domain.party.repository;

import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyChallengeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartyChallengeRepository extends JpaRepository<PartyChallenge, PartyChallengeId> {

    // PAR-01: 1개 파티 = 1개 챌린지 연계
    @Query("""
            SELECT pc
            FROM PartyChallenge pc
            JOIN FETCH pc.challenge
            WHERE pc.party.id = :partyId
            """)
    Optional<PartyChallenge> findByParty_Id(@Param("partyId") Long partyId);
}
