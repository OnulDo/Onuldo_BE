package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationListItemResDto(
        @Schema(example = "1")
        Long notificationId,
        @Schema(example = "VERIFICATION_DEADLINE")
        NotificationType type,
        @Schema(example = "1")
        Long challengeId,
        @Schema(example = "3")
        Long partyId,
        @Schema(example = "인증 마감 30분 전이에요")
        String title,
        @Schema(example = "30분 러닝 챌린지 인증을 잊지 마세요")
        String content,
        @Schema(example = "방금")
        String timeAgo,
        @Schema(example = "2026-08-09T00:11:38")
        LocalDateTime createdAt
) {
}
