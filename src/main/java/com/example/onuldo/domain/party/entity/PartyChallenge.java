package com.example.onuldo.domain.party.entity;

import com.example.onuldo.domain.challenge.entity.Challenge;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "party_challenge")
public class PartyChallenge {

    @EmbeddedId
    private PartyChallengeId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("partyId")
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("challengeId")
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;
}
