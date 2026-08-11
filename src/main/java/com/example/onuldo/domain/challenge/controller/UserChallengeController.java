package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.UserChallengeControllerDoc;
import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeRecordSummaryResDto;
import com.example.onuldo.domain.challenge.dto.response.OngoingChallengeRecordResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyCompletedChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.service.ParticipationService;
import com.example.onuldo.domain.challenge.service.ParticipationRecordService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPaginationDto;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/users/me")
public class UserChallengeController implements UserChallengeControllerDoc {

    private final ParticipationService participationService;
    private final ParticipationRecordService participationRecordService;

    @GetMapping("/challenges")
    public CursorPageResponse<UserChallengeResDto> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status,
            @Valid
            @ParameterObject
            CursorPaginationDto pagination
    ) {
        return participationService.getUserChallenges(userId, status, pagination.getCursor(), pagination.getSize());
    }

    @GetMapping("/challenges/daily")
    public BaseResponse<List<DailyChallengeResDto>> getDailyChallenges(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(participationService.getDailyChallenges(userId));
    }

    @GetMapping("/challenges/daily/completed")
    public BaseResponse<DailyCompletedChallengeListResDto> getDailyCompletedChallenges(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(participationService.getDailyCompletedChallenges(userId));
    }

    @GetMapping("/challenges/records/ongoing")
    public BaseResponse<List<OngoingChallengeRecordResDto>> getOngoingChallengeRecords(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(participationRecordService.getOngoingChallengeRecords(userId));
    }

    @GetMapping("/challenges/records/completed")
    public BaseResponse<CompletedChallengeRecordSummaryResDto> getCompletedChallengeRecords(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(participationRecordService.getCompletedChallengeRecords(userId));
    }
}
