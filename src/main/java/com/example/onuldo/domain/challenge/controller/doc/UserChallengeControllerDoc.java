package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = "참가 상태를 기준으로 챌린지 참여 정보를 조회합니다. status가 없으면 모든 상태를 조회합니다."
    )
    BaseResponse<UserChallengeListResDto> getUserChallenges(
            HttpServletRequest request,
            @RequestParam(required = false)
            ParticipationStatus status
    );
}
