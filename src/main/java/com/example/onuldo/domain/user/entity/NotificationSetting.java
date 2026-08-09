package com.example.onuldo.domain.user.entity;

import com.example.onuldo.domain.user.enums.NotificationSettingType;
import com.example.onuldo.domain.user.enums.NotificationType;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
@Table(name = "notification_setting")
public class NotificationSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "all_enabled", nullable = false)
    private Boolean allEnabled = true;

    @Builder.Default
    @Column(name = "challenge_start", nullable = false)
    private Boolean challengeStart = true;

    @Builder.Default
    @Column(name = "challenge_end_reminder", nullable = false)
    private Boolean challengeEndReminder = true;

    @Builder.Default
    @Column(name = "verification_deadline", nullable = false)
    private Boolean verificationDeadline = true;

    @Builder.Default
    @Column(name = "verification_result", nullable = false)
    private Boolean verificationResult = true;

    @Builder.Default
    @Column(name = "refund_complete", nullable = false)
    private Boolean refundComplete = true;

    @Builder.Default
    @Column(name = "party_member_verified", nullable = false)
    private Boolean partyMemberVerified = true;

    @Builder.Default
    @Column(name = "deduction_alert", nullable = false)
    private Boolean deductionAlert = true;

    public void apply(NotificationSettingType type, boolean enabled) {
        switch (type) {
            case ALL_ENABLED -> applyAll(enabled);
            case CHALLENGE_START -> challengeStart = enabled;
            case VERIFICATION_DEADLINE -> verificationDeadline = enabled;
            case CHALLENGE_END_REMINDER -> challengeEndReminder = enabled;
            case VERIFICATION_RESULT -> verificationResult = enabled;
            case PARTY_MEMBER_VERIFIED -> partyMemberVerified = enabled;
            case SETTLEMENT_COMPLETE -> {
                if (!enabled) {
                    throwRequiredSettlementNotificationException();
                }
                refundComplete = true;
            }
            default -> throw new RestApiException(GlobalErrorStatus._BAD_REQUEST);
        }
    }

    public boolean isEnabled(NotificationType type) {
        if (type == NotificationType.REFUND_COMPLETE || type == NotificationType.PARTY_SETTLEMENT_COMPLETE) {
            return refundComplete;
        }

        if (!allEnabled) {
            return false;
        }

        return switch (type) {
            case CHALLENGE_START -> challengeStart;
            case VERIFICATION_DEADLINE -> verificationDeadline;
            case CHALLENGE_END_REMINDER -> challengeEndReminder;
            case VERIFICATION_RESULT -> verificationResult;
            case PARTY_MEMBER_VERIFIED -> partyMemberVerified;
            case REFUND_COMPLETE, PARTY_SETTLEMENT_COMPLETE -> refundComplete;
        };
    }

    private void applyAll(boolean enabled) {
        allEnabled = enabled;
        challengeStart = enabled;
        challengeEndReminder = enabled;
        verificationDeadline = enabled;
        verificationResult = enabled;
        partyMemberVerified = enabled;
        deductionAlert = enabled;
        refundComplete = true;
    }

    private void throwRequiredSettlementNotificationException() {
        throw new RestApiException(GlobalErrorStatus._SETTLEMENT_COMPLETE_NOTIFICATION_REQUIRED);
    }
}
