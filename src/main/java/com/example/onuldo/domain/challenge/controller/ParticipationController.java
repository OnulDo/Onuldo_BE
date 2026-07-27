package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.ParticipationControllerDoc;
import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.service.ParticipationService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
public class ParticipationController implements ParticipationControllerDoc {

    private final ParticipationService participationService;

    @PostMapping("/{challengeId}/participations")
    public BaseResponse<ParticipationResDto> participatePersonalChallenge(
            @AuthUser
            Long userId,
            @PathVariable
            Long challengeId,
            @Valid
            @RequestBody
            ParticipationReqDto participationReqDto
    ) {
        ParticipationResDto result = participationService.participatePersonalChallenge(
                userId,
                challengeId,
                participationReqDto
        );

        return BaseResponse.onSuccess(result);
    }
}
