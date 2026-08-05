package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyHomeCardStatus;
import com.example.onuldo.domain.party.enums.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record PartyListResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "새벽 러너 파티")
        String name,
        @Schema(example = "30일 헬스")
        String challengeTitle,
        @Schema(example = "30분 러닝")
        String goal,
        @Schema(example = "ONGOING")
        PartyStatus status,
        @Schema(description = "오늘 나의 인증 상태 (홈 카드 정책과 동일)", example = "NOT_VERIFIED")
        PartyHomeCardStatus myStatus,
        @Schema(example = "2026-08-20")
        LocalDate endDate,
        @Schema(example = "12")
        Integer dDay,
        @Schema(example = "07:00:00")
        LocalTime verificationDeadline,
        @Schema(example = "0.72")
        Double progressRate,
        @Schema(example = "3")
        Integer verifiedMemberCount,
        @Schema(example = "5")
        Integer totalMemberCount,
        List<PartyMemberVerificationResDto> members
) {
}
