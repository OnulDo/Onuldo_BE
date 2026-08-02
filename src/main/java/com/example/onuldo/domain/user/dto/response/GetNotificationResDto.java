package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GetNotificationResDto (
        @Schema(example = "true")
        Boolean allEnabled,
        @Schema(example = "true")
        Boolean verificationDeadline,
        @Schema(example = "false")
        Boolean verificationResult,
        @Schema(example = "true")
        Boolean challengeStart,
        @Schema(example = "false")
        Boolean refundComplete,
        @Schema(example = "true")
        Boolean deductionAlert
) {

}
