package com.example.onuldo.domain.party.entity;

import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyMemberStatus;
import com.example.onuldo.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "party_member")
public class PartyMember {

    @EmbeddedId
    private PartyMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("partyId")
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyMemberRole role = PartyMemberRole.MEMBER;

    // PAR-06: 대기방에서 파티원별 준비완료/대기 상태 표시를 위해 추가
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyMemberStatus status = PartyMemberStatus.WAITING;

    @Builder.Default
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    // PAR-05: 파티원은 대기방에서 [준비완료]로 상태 전환
    public void ready() {
        this.status = PartyMemberStatus.READY;
    }

    public void waiting() {
        this.status = PartyMemberStatus.WAITING;
    }

    public boolean isReady() {
        return this.status == PartyMemberStatus.READY;
    }
}
