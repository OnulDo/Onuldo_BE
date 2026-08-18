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
    @Column(name = "settlement_id")
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

    // 예치금 환급분만(보너스·파티 분배금 제외) — 화면에 "도전금 환급"으로 별도 표시하기 위해 정산 시점에 저장
    @Builder.Default
    @Column(name = "deposit_refund_amount", nullable = false)
    private Integer depositRefundAmount = 0;

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

    // 홈 "정산이 완료됐어요!" 배너 노출 여부 — 정산 결과 화면을 조회하면 확인 처리되어 배너에서 사라진다.
    @Builder.Default
    @Column(name = "confirmed", nullable = false)
    private boolean confirmed = false;

    public void confirm() {
        this.confirmed = true;
    }
}
