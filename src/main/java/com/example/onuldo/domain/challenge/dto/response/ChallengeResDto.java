package com.example.onuldo.domain.challenge.dto.response;

import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.global.dto.response.ContentBlockResDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalTime;
import java.util.List;

@Builder
public record ChallengeResDto(
        @Schema(example = "1")
        Long id,
        @Schema(example = "30일 걷기 챌린지")
        String name,
        @Schema(example = "하루 30분 걷기")
        String explainContent,
        @Schema(example = "[{\"type\":\"h2\",\"content\":\"이 챌린지는?\"}]")
        List<ContentBlockResDto> description,
        @Schema(example = "https://cdn.onuldo.com/challenges/1.png")
        String captionImgUrl,
        @Schema(example = "걸음 수를 인증해주세요.")
        String verifyMethodContent,
        @Schema(example = "https://cdn.onuldo.com/challenges/1-example.png")
        String verificationExamplePhotoUrl,
        @Schema(example = "120")
        Integer participantCount,
        @Schema(example = "FITNESS")
        ChallengeCategory category,
        @Schema(example = "06:00:00")
        LocalTime timeStart,
        @Schema(example = "23:59:59")
        LocalTime timeEnd,
        @Schema(example = "[2, 4, 8, 12]")
        List<Integer> durationOptionList,
        @Schema(example = "[10000, 20000, 30000, 50000]")
        List<Integer> depositOptionList,
        @Schema(example = "[\"매일 인증하기\"]")
        List<String> successConditionList,
        @Schema(example = "[\"3회 미인증 시 실패\"]")
        List<String> failureConditionList,
        @Schema(example = "[\"걸음 수 인증\"]")
        List<String> verificationLabelList
) {
}
