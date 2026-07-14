package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.ChallengePoolStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "challenge_pool")
public class ChallengePool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_pool_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Builder.Default
    @Column(name = "total_deposit", nullable = false)
    private Long totalDeposit = 0L;

    @Builder.Default
    @Column(name = "total_refunded", nullable = false)
    private Long totalRefunded = 0L;

    @Builder.Default
    @Column(name = "forfeit_amount", nullable = false)
    private Long forfeitAmount = 0L;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengePoolStatus status = ChallengePoolStatus.OPEN;
}
