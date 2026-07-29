package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.UserChallengeControllerDoc;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.service.ParticipationService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserChallengeController implements UserChallengeControllerDoc {

    private final ParticipationService participationService;

    @GetMapping("/challenges")
    public BaseResponse<UserChallengeListResDto> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status
    ) {
        return BaseResponse.onSuccess(participationService.getUserChallenges(userId, status));
    }

    @GetMapping("/challenges/daily")
    public BaseResponse<DailyChallengeListResDto> getDailyChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = true)
            LocalDate date
    ) {
        return BaseResponse.onSuccess(participationService.getDailyChallenges(userId, date));
    }
}
