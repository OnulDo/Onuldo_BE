package com.example.onuldo.domain.user.controller;

import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.service.UserService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    @GetMapping("/notification")
    public BaseResponse<GetNotificationResDto> getNotification(
            @AuthUser Long userId
    ) {
        return BaseResponse.onSuccess(userService.getNotification(userId));
    }

    @PatchMapping("/notification")
    public BaseResponse<UpdateNotificationResDto> updateNotification(
            @AuthUser Long userId,
            @Valid @RequestBody UpdateNotificationReqDto request
    ) {
        return BaseResponse.onSuccess(userService.updateNotification(userId, request));
    }
}