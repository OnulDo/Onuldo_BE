package com.example.onuldo.domain.challenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "ad_reward_pool")
public class AdRewardPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month", nullable = false)
    private Integer yearMonth;

    @Builder.Default
    @Column(name = "ad_revenue", nullable = false)
    private Long adRevenue = 0L;

    @Builder.Default
    @Column(name = "bonus_paid", nullable = false)
    private Long bonusPaid = 0L;

    @Builder.Default
    @Column(name = "operating_cost", nullable = false)
    private Long operatingCost = 0L;
}
