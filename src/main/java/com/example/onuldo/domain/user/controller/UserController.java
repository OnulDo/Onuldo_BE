package com.example.onuldo.domain.user.controller;

import com.example.onuldo.domain.user.controller.doc.UserControllerDoc;
import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateProfileReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.GetMyPageResDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.GetProfileResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateProfileResDto;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.service.PointService;
import com.example.onuldo.domain.user.service.UserService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController implements UserControllerDoc {

    private final PointService pointService;
    private final UserService userService;

    @GetMapping("/profile")
    public BaseResponse<GetProfileResDto> getProfile(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess("프로필 조회에 성공했습니다.", userService.getProfile(userId));
    }

    @GetMapping
    public BaseResponse<GetMyPageResDto> getMyPage(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess("마이페이지 메인 조회에 성공했습니다.", userService.getMyPage(userId));
    }

    @PatchMapping ("/profile")
    public BaseResponse<UpdateProfileResDto> updateProfile(
            @AuthUser
            Long userId,
            @RequestBody
            UpdateProfileReqDto request
    ) {
        return BaseResponse.onSuccess(userService.updateProfile(userId, request));
    }

    @GetMapping("/notification-settings")
    public BaseResponse<GetNotificationResDto> getNotification(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(userService.getNotification(userId));
    }

    @PatchMapping("/notification-settings")
    public BaseResponse<UpdateNotificationResDto> updateNotification(
            @AuthUser
            Long userId,
            @Valid
            @RequestBody
            UpdateNotificationReqDto request
    ) {
        return BaseResponse.onSuccess(userService.updateNotification(userId, request));
    }

    @PostMapping("/wallet/charges")
    public BaseResponse<ChargePointResDto> chargePoint(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            ChargePointReqDto request
    ) {
        return BaseResponse.onSuccess(pointService.chargePoint(userId, request));
    }

    @PostMapping("/wallet/signup-bonuses")
    public BaseResponse<ChargePointResDto> grantSignupBonus(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(pointService.grantSignupBonus(userId));
    }

    @GetMapping("/wallet/summary")
    public BaseResponse<PointWalletSummaryResDto> getPointWalletSummary(
            @AuthUser
            Long userId
    ) {
        return BaseResponse.onSuccess(pointService.getPointWalletSummary(userId));
    }

    @GetMapping("/wallet/transactions")
    public CursorPageResponse<PointTransactionResDto> getPointTransactions(
            @AuthUser
            Long userId,

            @RequestParam(required = false)
            PointTransactionType type,

            @RequestParam(required = false)
            String cursor,

            @RequestParam(defaultValue = "" + CursorConstants.DEFAULT_SIZE)
            int size
    ) {
        return pointService.getPointTransactions(userId, type, cursor, size);
    }
}
