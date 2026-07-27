package com.example.onuldo.domain.challenge.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ParticipationReqDto(
        @Schema(example = "30000")
        @NotNull(message = "도전금은 필수입니다.")
        Integer depositAmount,

        @Schema(example = "4")
        @NotNull(message = "진행 기간은 필수입니다.")
        @Min(value = 1, message = "진행 기간은 1주 이상이어야 합니다.")
        Integer durationWeeks
) {
}
