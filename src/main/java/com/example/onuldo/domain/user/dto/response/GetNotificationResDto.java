package com.example.onuldo.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GetNotificationResDto (
        @Schema(example = "true")
        Boolean allEnabled,
        @Schema(example = "true")
        Boolean verificationDeadline,
        @Schema(example = "true")
        Boolean challengeStart,
        @Schema(example = "true")
        Boolean challengeEndReminder,
        @Schema(example = "true")
        Boolean verificationResult,
        @Schema(example = "true")
        Boolean partyMemberVerified,
        @Schema(example = "true")
        Boolean settlementComplete
) {

}
