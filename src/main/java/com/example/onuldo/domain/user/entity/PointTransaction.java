package com.example.onuldo.domain.user.entity;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.user.enums.PointTransactionType;
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
import com.example.onuldo.global.common.time.TimeService;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_transaction")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointTransactionType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "deposit_amount")
    private Integer depositAmount;

    @Column(name = "adjustment_amount")
    private Integer adjustmentAmount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "description", nullable = false)
    private String description;

    // 정산(REFUND) 결과 성공/실패 — FE가 문자열 파싱 없이 색상을 분기할 수 있도록 별도 필드로 기록.
    // 정산 외 거래(충전/출금/예치)에는 해당 없어 null.
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 20)
    private ParticipationStatus resultStatus;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = TimeService.nowKstStatic();
}
