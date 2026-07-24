package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record ChallengeResDto(
        Long id,
        String name,
        String explainContent,
        String description,
        String captionImgUrl,
        String verifyMethodContent,
        String verificationExamplePhotoUrl,
        Integer participantCount,
        ChallengeCategory category,
        LocalTime timeStart,
        LocalTime timeEnd,
        List<Integer> durationOptionList,
        List<Integer> depositOptionList,
        List<String> successConditionList,
        List<String> failureConditionList,
        List<String> verificationLabelList
) {
}
