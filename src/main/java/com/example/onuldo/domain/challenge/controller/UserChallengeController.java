package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.UserChallengeControllerDoc;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.service.ParticipationService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserChallengeController implements UserChallengeControllerDoc {

    private final ParticipationService participationService;

    @GetMapping("/challenges")
    public BaseResponse<UserChallengeListResDto> getUserChallenges(
            HttpServletRequest request,
            @RequestParam(required = false)
            ParticipationStatus status
    ) {
        Long userId = (Long) request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE);
        return BaseResponse.onSuccess(participationService.getUserChallenges(userId, status));
    }
}
