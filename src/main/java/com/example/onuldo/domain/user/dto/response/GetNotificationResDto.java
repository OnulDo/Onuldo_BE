package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

@Builder
public record GetNotificationResDto (
        Boolean allEnabled,
        Boolean verificationDeadline,
        Boolean verificationResult,
        Boolean challengeStart,
        Boolean refundComplete,
        Boolean deductionAlert
) {

}
