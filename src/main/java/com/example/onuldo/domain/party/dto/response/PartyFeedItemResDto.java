package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyFeedItemResDto(
        @Schema(example = "5")
        Long userId,
        @Schema(example = "김민지")
        String nickname,
        @Schema(example = "https://cdn.onuldo.com/profile/5.png", nullable = true)
        String profileImageUrl,
        @Schema(example = "true")
        boolean isVerifiedToday,
        @Schema(example = "https://cdn.onuldo.com/verifications/1.png", nullable = true)
        String verificationPhotoUrl,
        @Schema(example = "2026-07-23T08:10:00", nullable = true)
        LocalDateTime verifiedAt
) { }
