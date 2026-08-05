package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChallengeManualReviewResDto(
        @Schema(example = "2026-08-03T09:30:00")
        LocalDateTime manualReviewRequestedAt
) {
}
