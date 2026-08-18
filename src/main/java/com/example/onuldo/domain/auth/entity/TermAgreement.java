package com.example.onuldo.domain.auth.entity;

import com.example.onuldo.domain.user.entity.User;
import jakarta.persistence.Column;
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

import java.time.LocalDateTime;
import com.example.onuldo.global.common.time.TimeService;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "term_agreement")
public class TermAgreement {

    @EmbeddedId
    private TermAgreementId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("termId")
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Builder.Default
    @Column(name = "agreed", nullable = false)
    private Boolean agreed = false;

    @Builder.Default
    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt = TimeService.nowKstStatic();
}
