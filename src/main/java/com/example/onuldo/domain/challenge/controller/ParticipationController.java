package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.ParticipationControllerDoc;
import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.service.ParticipationService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/{challengeId}/participate")
    public BaseResponse<ParticipationResDto> participatePersonalChallenge(
            HttpServletRequest request,
            @PathVariable
            Long challengeId,
            @Valid
            @RequestBody
            ParticipationReqDto participationReqDto
    ) {
        Long userId = (Long) request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE);
        ParticipationResDto result = participationService.participatePersonalChallenge(
                userId,
                challengeId,
                participationReqDto
        );

        return BaseResponse.onSuccess(result);
    }
}
