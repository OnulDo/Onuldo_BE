package com.example.onuldo.domain.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChallengeContentBlockDto(
        @Schema(example = "h2")
        String type,
        @Schema(example = "이 챌린지는?")
        String content
) {
}
