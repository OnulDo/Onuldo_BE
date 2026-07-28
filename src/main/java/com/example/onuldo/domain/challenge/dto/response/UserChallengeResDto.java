package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.dto.ChallengeContentBlockDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record UserChallengeResDto(
        @Schema(example = "1")
        Long participationId,
        @Schema(example = "ONGOING")
        ParticipationStatus participationStatus,
        @Schema(example = "PERSONAL")
        ParticipationType participationType,
        @Schema(example = "12")
        Long challengeId,
        @Schema(example = "30일 걷기 챌린지")
        String challengeName,
        @Schema(example = "하루 30분 걷기")
        String challengeExplainContent,
        @Schema(example = "[{\"type\":\"h2\",\"content\":\"이 챌린지는?\"}]")
        List<ChallengeContentBlockDto> challengeDescription,
        @Schema(example = "https://cdn.onuldo.com/challenges/1.png")
        String challengeCaptionImgUrl,
        @Schema(example = "걸음 수를 인증해주세요.")
        String challengeVerifyMethodContent,
        @Schema(example = "https://cdn.onuldo.com/challenges/1-example.png")
        String challengeVerificationExamplePhotoUrl,
        @Schema(example = "120")
        Integer participantCount,
        @Schema(example = "FITNESS")
        ChallengeCategory category,
        @Schema(example = "06:00:00")
        LocalTime timeStart,
        @Schema(example = "23:59:59")
        LocalTime timeEnd,
        @Schema(example = "30000")
        Integer depositAmount,
        @Schema(example = "4")
        Integer durationWeeks,
        @Schema(example = "2026-07-27")
        LocalDate startDate,
        @Schema(example = "2026-08-24")
        LocalDate endDate
) {
}
