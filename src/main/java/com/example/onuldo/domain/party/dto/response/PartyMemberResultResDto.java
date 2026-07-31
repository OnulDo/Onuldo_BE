package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyMemberResultResDto(
        @Schema(example = "5")
        Long userId,
        @Schema(example = "오늘두")
        String nickname,
        @Schema(example = "https://cdn.onuldo.com/profile/5.png", nullable = true)
        String profileImageUrl,
        @Schema(example = "SUCCESS")
        ParticipationStatus status,
        @Schema(example = "10000")
        Integer displayAmount
) {
}
