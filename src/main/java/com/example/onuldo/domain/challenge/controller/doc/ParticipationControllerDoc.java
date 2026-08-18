package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.config.swagger.ApiErrorCodes;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Challenge")
public interface ParticipationControllerDoc {

    @Operation(
            summary = "개인 유저의 챌린지 참여",
            description = "도전금과 진행 기간을 입력받아 개인 챌린지 참여를 생성하고 포인트를 차감합니다."
    )
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._UNAUTHORIZED,
            ErrorStatus._INVALID_TOKEN,
            ErrorStatus._TOKEN_EXPIRED,
            ErrorStatus._USER_NOT_FOUND,
            ErrorStatus._CHALLENGE_NOT_FOUND,
            ErrorStatus._INVALID_DURATION_OPTION,
            ErrorStatus._INVALID_DEPOSIT_OPTION,
            ErrorStatus._ALREADY_PARTICIPATING_CHALLENGE,
            ErrorStatus._CHALLENGE_PARTY_ALREADY_WAITING,
            ErrorStatus._INSUFFICIENT_POINT_FOR_CHALLENGE,
            ErrorStatus._CHALLENGE_POT_NOT_FOUND,
            ErrorStatus._PARTICIPATION_PARTY_NOT_ALLOWED,
            ErrorStatus._PARTICIPATION_PARTY_REQUIRED
    })
    BaseResponse<ParticipationResDto> participatePersonalChallenge(
            @AuthUser
            Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody ParticipationReqDto participationReqDto
    );
}
