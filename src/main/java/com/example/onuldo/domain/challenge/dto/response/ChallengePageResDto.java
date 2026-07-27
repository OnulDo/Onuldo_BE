package com.example.onuldo.domain.challenge.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record ChallengePageResDto(
        @Schema(description = "챌린지 목록")
        List<ChallengeResDto> challenges,
        @Schema(example = "0")
        int page,
        @Schema(example = "10")
        int size,
        @Schema(example = "120")
        long totalElements,
        @Schema(example = "12")
        int totalPages,
        @Schema(example = "true")
        boolean hasNext
) {
}
