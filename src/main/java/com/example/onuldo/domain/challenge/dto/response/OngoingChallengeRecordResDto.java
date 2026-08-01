package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ParticipationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record OngoingChallengeRecordResDto(
        @Schema(example = "1")
        Long participationId,
        @Schema(example = "12")
        Long challengeId,
        @Schema(example = "매일 6시 기상")
        String challengeTitle,
        @Schema(example = "false")
        Boolean isVerifiedToday,
        @Schema(example = "13")
        Integer daysUntilEnd,
        @Schema(example = "72")
        Integer achievementRate,
        @Schema(example = "30000")
        Integer depositAmount,
        @Schema(example = "PERSONAL")
        ParticipationType type
) {
}
