package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CompletedChallengeResDto (
        @Schema(example = "1")
        Long participationId,
        @Schema(example = "1")
        Long challengeId,
        @Schema(example = "매일 6시 기상")
        String challengeName,
        @Schema(example = "2026-07-31T06:10:00")
        LocalDateTime verifiedAt,
        @Schema(example = "15")
        Integer streakDays
){

}
