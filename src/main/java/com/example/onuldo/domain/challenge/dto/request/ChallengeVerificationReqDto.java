package com.example.onuldo.domain.challenge.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ChallengeVerificationReqDto(
        @Schema(example = "uploads/2026/07/28/018fffd1-7fc0-7bdd-9e9e-3c5cc3f2d3a0.jpg")
        @NotBlank(message = "fileId는 필수입니다.")
        String fileId
) {
}
