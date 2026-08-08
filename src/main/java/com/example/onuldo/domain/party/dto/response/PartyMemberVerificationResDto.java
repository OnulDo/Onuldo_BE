package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyMemberVerificationResDto(
        @Schema(example = "5")
        Long userId,
        @Schema(example = "김민지")
        String nickname,
        @Schema(example = "https://cdn.onuldo.com/profile/5.png", nullable = true)
        String profileImageUrl,
        @Schema(example = "true")
        boolean isVerifiedToday
) {
}
