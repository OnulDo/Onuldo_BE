package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record UserChallengeResDto(
        Long participationId,
        ParticipationStatus participationStatus,
        ParticipationType participationType,
        Long challengeId,
        String challengeName,
        String challengeExplainContent,
        String challengeDescription,
        String challengeCaptionImgUrl,
        String challengeVerifyMethodContent,
        String challengeVerificationExamplePhotoUrl,
        Integer participantCount,
        ChallengeCategory category,
        LocalTime timeStart,
        LocalTime timeEnd,
        Integer depositAmount,
        Integer durationWeeks,
        LocalDate startDate,
        LocalDate endDate
) {
}
