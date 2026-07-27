package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record PartyListResDto(
        @Schema(example = "101")
        Long partyId,
        @Schema(example = "30일 헬스 챌린지 파티")
        String name,
        @Schema(example = "30일 헬스")
        String challengeTitle,
        @Schema(example = "ONGOING")
        PartyStatus status,
        @Schema(example = "2026-08-20")
        LocalDate endDate,
        @Schema(example = "21:00:00")
        LocalTime verificationDeadline,
        @Schema(example = "0.72")
        Double progressRate,
        @Schema(example = "3")
        Integer verifiedMemberCount,
        @Schema(example = "4")
        Integer totalMemberCount
) {
}
