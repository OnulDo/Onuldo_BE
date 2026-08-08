package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyHomeMemberResDto(
        @Schema(example = "5")
        Long userId,
        @Schema(example = "https://cdn.onuldo.com/profile/5.png")
        String profileImageUrl,
        // HOME-07: 인증 완료 = 테두리 + 불투명도 100% / 미인증 = 테두리 없음 + 불투명도 50%
        @Schema(example = "true")
        boolean isVerifiedToday
) {
}
