package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CompletedChallengeRecordResDto(
        @Schema(example = "1")
        Long participationId,
        @Schema(example = "12")
        Long challengeId,
        @Schema(example = "매일 6시 기상")
        String challengeTitle,
        @Schema(example = "SUCCESS")
        ParticipationStatus resultStatus,
        @Schema(example = "30000")
        Integer depositAmount,
        @Schema(example = "2500")
        Integer adjustmentAmount,
        @Schema(example = "2026-08-10")
        LocalDate endDate,
        @Schema(example = "92")
        Integer achievementRate
) {
}
