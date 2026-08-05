package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyHomeCardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Builder
public record PartyHomeItemResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "새벽 러너 파티")
        String name,
        @Schema(example = "30분 러닝")
        String challengeTitle,
        @Schema(example = "2026-08-20")
        LocalDate endDate,
        @Schema(example = "07:00")
        LocalTime verificationDeadline,
        // HOME-03: 남은 시간 칩은 인증 마감 1시간 전부터 표시
        @Schema(example = "true")
        boolean showRemainingTime,
        @Schema(example = "NOT_VERIFIED")
        PartyHomeCardStatus status,
        @Schema(example = "2026-07-23T09:00:00", nullable = true)
        LocalDateTime verifiedAt,
        List<PartyHomeMemberResDto> members
) {
}
