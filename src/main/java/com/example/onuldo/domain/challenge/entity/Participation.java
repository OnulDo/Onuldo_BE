package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "participation")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "participation_type", nullable = false, length = 20)
    private ParticipationType participationType = ParticipationType.PERSONAL;

    @Column(name = "deposit_amount", nullable = false)
    private Integer depositAmount;

    // 참가 생성 시점의 ChallengePot.currentBonusRate 스냅샷 — 이후 β가 재조정돼도 이 참가는 가입 시점 값으로 정산된다.
    // PARTY 참가·이 기능 이전에 생성된 레거시 PERSONAL 참가는 null.
    @Column(name = "applied_bonus_rate", precision = 5, scale = 4)
    private BigDecimal appliedBonusRate;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status = ParticipationStatus.ONGOING;

    public void changeStatus(ParticipationStatus status) {
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    private void validateParticipationType() {
        if (participationType == ParticipationType.PERSONAL && party != null) {
            throw new RestApiException(ErrorStatus._PARTICIPATION_PARTY_NOT_ALLOWED);
        }

        if (participationType == ParticipationType.PARTY && party == null) {
            throw new RestApiException(ErrorStatus._PARTICIPATION_PARTY_REQUIRED);
        }
    }
}
