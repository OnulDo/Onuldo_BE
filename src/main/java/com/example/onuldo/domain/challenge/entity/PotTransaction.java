package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.PotTransactionType;
import com.example.onuldo.global.common.time.TimeService;
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

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pot_transaction")
public class PotTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pot_transaction_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PotTransactionType type;

    // 항상 양수, 부호(적립/차감)는 type으로 판단
    @Column(nullable = false)
    private Integer amount;

    // 트랜잭션 처리 직후 pot 잔액 스냅샷 (음수 가능)
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    // settlement.getParticipation()으로 유저·챌린지까지 추적 가능해 별도 FK를 두지 않음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = TimeService.nowKstStatic();
}
