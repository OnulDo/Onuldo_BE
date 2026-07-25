package com.example.onuldo.domain.party.entity;

import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.user.entity.User;
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
@Table(name = "party")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyStatus status = PartyStatus.WAITING;

    @Builder.Default
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 5;

    // PAR-06: 대기방 헤더에 진행 기간 표시, PAR-05: 파티 시작 시 파티원 전원 도전금 예치에 사용
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    // PAR-06: 대기방 헤더에 1인 도전금 표시, PAR-05: 파티 시작 시 파티원 전원 도전금 예치에 사용
    @Column(name = "deposit_amount", nullable = false)
    private Integer depositAmount;

    @Column(name = "start_triggered_at")
    private LocalDateTime startTriggeredAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public void updateStatus(PartyStatus status) {
        this.status = status;
    }

    public void updateStartTriggeredAt(LocalDateTime startTriggeredAt) {
        this.startTriggeredAt = startTriggeredAt;
    }

    public void updateInviteExpiresAt(LocalDateTime inviteExpiresAt) {
        this.inviteExpiresAt = inviteExpiresAt;
    }
}
