package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyCompletedChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = """
    참가 상태를 기준으로 챌린지 참여 정보를 조회합니다. 
    status가 없으면 모든 상태를 조회합니다.
    
    참여중인 챌린지의 상세 설명 json의 type 값은 다음으로 구성됩니다.
        - h2
        - h3
        - paragraph
        - linebreak
        - blockquote
    """
    )
    BaseResponse<UserChallengeListResDto> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status
    );

    @Operation(
            summary = "지정한 날짜의 챌린지 조회",
            description = """
    지정한 날짜 기준으로 수행해야 하는 챌린지를 조회합니다.
    조건은 `ONGOING` 상태이면서 startDate <= date <= endDate 입니다.

    각 챌린지에는 해당 날짜 인증 기록이 있는지 여부(`verifiedOnDate`)가 포함됩니다.
    """
    )
    BaseResponse<DailyChallengeListResDto> getDailyChallenges(
            @Parameter(hidden = true)
            @AuthUser
            Long userId,
            @Parameter(description = "조회할 날짜", example = "2026-07-29")
            @RequestParam(required = true)
            LocalDate date
    );

    @Operation(
            summary = "오늘 완료한 챌린지 목록 조회",
            description = """
    오늘 진행 중이던 챌린지 중 인증까지 완료된 항목을 파티(`parties`)/개인 챌린지(`challenges`)로 나눠 조회합니다.

    각 항목의 `verifiedAt`은 오늘 실제로 인증을 완료한 시각입니다(챌린지의 인증 마감 시각이 아님).

    정렬 기준
    - parties: 인증 시각(verifiedAt) 빠른 순
    - challenges: 연속 성공 일수(streak) 높은 순, 동일하면 인증 시각(verifiedAt) 빠른 순

    파티 항목은 총 참여 인원/오늘 인증 완료 인원을, 개인 챌린지 항목은 연속 성공 일수(streakDays)를 포함합니다.
    """
    )
    BaseResponse<DailyCompletedChallengeListResDto> getDailyCompletedChallenges(
            @Parameter(hidden = true)
            @AuthUser
            Long userId
    );
}
