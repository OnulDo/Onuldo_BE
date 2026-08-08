package com.example.onuldo.domain.challenge.entity;

import com.example.onuldo.global.common.time.TimeService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "challenge_pot")
public class ChallengePot {

    @Id
    @Column(name = "challenge_pot_id")
    private Long id;

    // 음수 허용 — 몰수금 적립보다 보너스 지급이 먼저 발생하면 운영자 임시 충당 상태로 마이너스가 될 수 있음
    @Builder.Default
    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    // 누적 몰수 합계 — 감사/통계용
    @Builder.Default
    @Column(name = "total_forfeited", nullable = false)
    private Long totalForfeited = 0L;

    // 누적 보너스 지급 합계 — 감사/통계용
    @Builder.Default
    @Column(name = "total_bonus_paid", nullable = false)
    private Long totalBonusPaid = 0L;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = TimeService.nowKstStatic();

    // 개인 챌린지 실패자 몰수금(도전금 - 환급액)을 pot 잔액에 적립
    public void accumulateForfeiture(long amount) {
        this.balance += amount;
        this.totalForfeited += amount;
        this.updatedAt = TimeService.nowKstStatic();
    }

    // 개인 챌린지 성공자 보너스만큼 pot 잔액에서 차감 (마이너스 허용)
    public void payBonus(long amount) {
        this.balance -= amount;
        this.totalBonusPaid += amount;
        this.updatedAt = TimeService.nowKstStatic();
    }
}
