package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordSummaryResDto;
import com.example.onuldo.domain.challenge.dto.response.OngoingChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = "참가 상태를 기준으로 챌린지 참여 정보를 조회합니다. status가 없으면 모든 상태를 조회합니다."
    )
    BaseResponse<UserChallengeListResDto> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status
    );

    @Operation(
            summary = "내 진행 중 챌린지 기록 조회",
            description = "진행 중 챌린지의 제목, 당일 인증 여부, 종료까지 남은 날짜, 달성률, 예상 환급금, 도전금, 타입을 조회합니다."
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
