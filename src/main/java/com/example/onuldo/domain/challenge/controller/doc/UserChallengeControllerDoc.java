package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordSummaryResDto;
import com.example.onuldo.domain.challenge.dto.response.OngoingChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyCompletedChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPaginationDto;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = """
                    커서 기반 페이지네이션으로 챌린지 참여 정보를 조회합니다.
                    참여 id가 최신순으로 정렬되며, cursor를 넘기지 않으면 첫 페이지를 반환합니다. 
                    status가 없으면 모든 상태를 조회합니다.
                    
                    참여중인 챌린지의 상세 설명 json의 type 값은 다음으로 구성됩니다.
                    - h2
                    - h3
                    - paragraph
                    - linebreak
                    - blockquote
                    """
    )
    CursorPageResponse<UserChallengeResDto> getUserChallenges(
            @AuthUser
            Long userId,

            @RequestParam(required = false)
            ParticipationStatus status,

            @Valid
            @ParameterObject
            CursorPaginationDto pagination
    );

    @Operation(
            summary = "오늘 날짜의 챌린지 조회",
            description = """
    오늘 날짜 기준으로 수행해야 하는 개인 챌린지를 조회합니다.
    조건은 참여 타입이 'PERSONAL'이고 `ONGOING` 상태이면서 startDate <= date <= endDate 입니다.
    파티 챌린지는 이 API에 포함되지 않습니다.

    각 챌린지에는 해당 날짜 인증 기록이 있는지 여부(`verifiedOnDate`)가 포함됩니다.
    """
    )
    BaseResponse<List<DailyChallengeResDto>> getDailyChallenges(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "오늘 완료한 챌린지 목록 조회",
            description = """
    오늘 진행 중이던 챌린지 중 인증까지 완료된 항목을 파티(`parties`)/개인 챌린지(`challenges`)로 나눠 조회합니다.

    각 항목의 `verifiedAt`은 오늘 실제로 인증을 완료한 시각입니다(챌린지의 인증 마감 시각이 아님).

    정렬 기준
    - parties: 인증 시각(verifiedAt) 빠른 순
    - challenges: 인증 시각(verifiedAt) 빠른 순

    파티 항목은 총 참여 인원/오늘 인증 완료 인원을, 개인 챌린지 항목은 연속 성공 일수(streakDays)를 포함합니다.
    """
    )
    BaseResponse<DailyCompletedChallengeListResDto> getDailyCompletedChallenges(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "내 진행 중 챌린지 기록 조회",
            description = "진행 중 챌린지의 제목, 당일 인증 여부, 종료까지 남은 날짜, 달성률, 도전금, 타입을 조회합니다."
    )
    BaseResponse<List<OngoingChallengeRecordResDto>> getOngoingChallengeRecords(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "내 완료 챌린지 기록 조회",
            description = "완료 챌린지의 총 완료 개수, 성공률, 총 적립, 성공/실패 목록, 환급금, 종료일, 달성률을 조회합니다."
    )
    BaseResponse<CompletedChallengeRecordSummaryResDto> getCompletedChallengeRecords(
            @AuthUser
            Long userId
    );
}
