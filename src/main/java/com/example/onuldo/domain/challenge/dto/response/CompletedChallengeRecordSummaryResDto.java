package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record CompletedChallengeRecordSummaryResDto(
        @Schema(example = "8")
        Integer totalCompletedCount,
        @Schema(example = "75")
        Integer successRate,
        @Schema(example = "210000")
        Long totalSavedAmount,
        @Schema(description = "완료한 챌린지 목록")
        List<CompletedChallengeRecordResDto> completedChallenges
) {
}
