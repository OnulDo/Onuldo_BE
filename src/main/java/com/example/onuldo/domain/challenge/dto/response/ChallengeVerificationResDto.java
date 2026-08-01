package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ChallengeVerificationResDto(
        @Schema(example = "1")
        Long verificationId,
        @Schema(example = "12")
        Long challengeId,
        @Schema(example = "5")
        Long participationId,
        @Schema(example = "uploads/2026/07/28/018fffd1-7fc0-7bdd-9e9e-3c5cc3f2d3a0.jpg")
        String fileId,
        @Schema(example = "2026-07-28")
        LocalDate verificationDate,
        @Schema(example = "2026-07-28T09:30:00")
        LocalDateTime verifiedAt,
        @Schema(example = "PASS")
        VerificationReviewStatus review
) {
}
