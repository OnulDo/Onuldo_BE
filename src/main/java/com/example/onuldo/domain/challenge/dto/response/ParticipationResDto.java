package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ParticipationResDto(
        @Schema(example = "2026-07-27")
        LocalDate startDate,
        @Schema(example = "2026-08-24")
        LocalDate endDate,
        @Schema(example = "4")
        Integer durationWeeks,
        @Schema(example = "28")
        Integer durationDays,
        @Schema(example = "30000")
        Integer depositAmount,
        @Schema(example = "35000")
        Integer expectedRefundAmount
) {
}
