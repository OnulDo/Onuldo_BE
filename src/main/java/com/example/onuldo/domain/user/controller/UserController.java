package com.example.onuldo.domain.user.controller;

import com.example.onuldo.domain.user.controller.doc.UserControllerDoc;
import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.response.GetMyPageResDto;
import com.example.onuldo.domain.user.dto.response.*;
import com.example.onuldo.domain.user.service.PointService;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.service.UserService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController implements UserControllerDoc {

    private final PointService pointService;
    private final UserService userService;

    @GetMapping
    public BaseResponse<GetMyPageResDto> getMyPage(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess("마이페이지 메인 조회에 성공했습니다.", userService.getMyPage(userId));
    }

    @GetMapping("/notification")
    public BaseResponse<GetNotificationResDto> getNotification(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(userService.getNotification(userId));
    }

    @PatchMapping("/notification")
    public BaseResponse<UpdateNotificationResDto> updateNotification(
            @AuthUser
            Long userId,
            @Valid
            @RequestBody
            UpdateNotificationReqDto request
    ) {
        return BaseResponse.onSuccess(userService.updateNotification(userId, request));
    }

    @PostMapping("/wallet/charge")
    public BaseResponse<ChargePointResDto> chargePoint(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            ChargePointReqDto request
    ) {
        return BaseResponse.onSuccess(pointService.chargePoint(userId, request));
    }

    @GetMapping("/wallet/summary")
    public BaseResponse<PointWalletSummaryResDto> getPointWalletSummary(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(pointService.getPointWalletSummary(userId));
    }

    @GetMapping("/wallet/transactions")
    public BaseResponse<PointTransactionScrollResDto> getPointTransactions(
            @AuthUser
            Long userId,

            @RequestParam(required = false)
            PointTransactionType type,

            @RequestParam(required = false)
            Long cursor,

            @Min(1)
            @RequestParam(defaultValue = "10")
            int size
    ){
        return BaseResponse.onSuccess(pointService.getPointTransactions(userId, type, cursor, size));
    }


}