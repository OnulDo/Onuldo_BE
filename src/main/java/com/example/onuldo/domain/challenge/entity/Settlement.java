package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.SettlementStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @Column(name = "deposit_amount", nullable = false)
    private Integer depositAmount;

    @Column(name = "r_value", precision = 5, scale = 2)
    private BigDecimal rValue;

    @Builder.Default
    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount = 0;

    @Builder.Default
    @Column(name = "bonus_amount", nullable = false)
    private Integer bonusAmount = 0;

    @Builder.Default
    @Column(name = "party_share_amount", nullable = false)
    private Integer partyShareAmount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
