package com.example.onuldo.domain.user.controller;

import com.example.onuldo.domain.user.controller.doc.UserControllerDoc;
import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionScrollResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.service.PointService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController implements UserControllerDoc {

    private final PointService pointService;

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
            Long cursor,

            @RequestParam(defaultValue = "10")
            int size
    ){
        return BaseResponse.onSuccess(pointService.getPointTransactions(userId, cursor, size));
    }


}
