package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationListItemResDto(
        @Schema(example = "1")
        Long notificationId,
        @Schema(
                description = """
                        알림 타입
                        - VERIFICATION_DEADLINE: 인증 마감 리마인더
                        - VERIFICATION_APPROVED: 인증 승인 결과
                        - VERIFICATION_REJECTED: 인증 기각 결과
                        - PARTY_MEMBER_VERIFIED: 파티원 인증 완료
                        - CHALLENGE_START: 새 챌린지 시작
                        - CHALLENGE_END_REMINDER: 종료일 리마인더
                        - REFUND_COMPLETE: 개인 챌린지 정산/환급 완료
                        - PARTY_SETTLEMENT_COMPLETE: 파티 정산 완료
                        """,
                example = "VERIFICATION_DEADLINE"
        )
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
