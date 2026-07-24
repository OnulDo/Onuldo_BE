package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Challenge")
public interface ParticipationControllerDoc {

    @Operation(
            summary = "개인 유저의 챌린지 참여",
            description = "도전금과 진행 기간을 입력받아 개인 챌린지 참여를 생성하고 포인트를 차감합니다."
    )
    BaseResponse<ParticipationResDto> participatePersonalChallenge(
            HttpServletRequest request,
            @PathVariable Long challengeId,
            @Valid @RequestBody ParticipationReqDto participationReqDto
    );
}
