package com.example.onuldo.domain.user.entity;

import com.example.onuldo.domain.user.enums.SocialProvider;
import com.example.onuldo.domain.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    @Setter
    private String nickname;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "social_id", length = 255)
    private String socialId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false, length = 20)
    private SocialProvider socialProvider = SocialProvider.EMAIL;

    @Column(name = "profile_image_url", length = 255)
    @Setter
    private String profileImageUrl;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    @Setter
    private Boolean emailVerified = false;

    @Builder.Default
    @Column(name = "point_balance", nullable = false)
    @Setter
    private Long pointBalance = 0L;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "withdrawal_requested_at")
    @Setter
    private LocalDateTime withdrawalRequestedAt;

    @Builder.Default
    @Column(name = "login_fail_count", nullable = false)
    @Setter
    private Integer loginFailCount = 0;

    @Column(name = "locked_until")
    @Setter
    private LocalDateTime lockedUntil;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    @Setter
    private LocalDateTime lastLoginAt;

}
