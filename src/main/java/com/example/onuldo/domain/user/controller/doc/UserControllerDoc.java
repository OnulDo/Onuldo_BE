package com.example.onuldo.domain.user.controller.doc;

import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionScrollResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "마이페이지 API")
public interface UserControllerDoc {

    @Operation(
            summary = "알림 설정 조회",
            description = "로그인한 유저의 알림 설정을 조회합니다."
    )
    BaseResponse<GetNotificationResDto> getNotification(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "알림 설정 변경",
            description = """
                    로그인한 유저의 특정 알림 타입 on/off 여부를 변경합니다.

                    알림 타입(type)
                    - CHALLENGE_START: 챌린지 시작 알림
                    - VERIFICATION_DEADLINE: 인증 마감 임박 알림
                    - VERIFICATION_RESULT: 인증 결과 알림
                    - REFUND_COMPLETE: 환급 완료 알림
                    - DEDUCTION_ALERT: 차감 알림
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "type": "CHALLENGE_START",
                          "enabled": false
                        }
                        """
                    )
            )
    )
    BaseResponse<UpdateNotificationResDto> updateNotification(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            UpdateNotificationReqDto request
    );

    @Operation(
            summary = "포인트 충전",
            description = "결제 수단을 통해 포인트를 충전하고 충전된 금액과 충전 후 잔액을 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "timestamp": "2026-07-24T15:28:37.047Z",
                          "code": "SUCCESS",
                          "message": "요청에 성공하였습니다.",
                          "result": {
                            "amount": 10000,
                            "balanceAfter": 10000
                          }
                        }
                        """
                    )
            )
    )
    BaseResponse<ChargePointResDto> chargePoint(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            ChargePointReqDto request
    );

    @Operation(
            summary = "포인트 지갑 요약 조회",
            description = "잔액, 예치 중인 포인트, 누적 예치액, 누적 환급액, 누적 차감액, 평균 환급률을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "timestamp": "2026-07-24T15:28:37.050Z",
                          "code": "SUCCESS",
                          "message": "요청에 성공하였습니다.",
                          "result": {
                            "balance": 10000,
                            "pendingPoints": 0,
                            "totalDeposit": 5000,
                            "totalRefund": 5000,
                            "totalPenalty": 500,
                            "averageReturnRate": 100
                          }
                        }
                        """
                    )
            )
    )
    BaseResponse<PointWalletSummaryResDto> getPointWalletSummary(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "포인트 거래 내역 조회",
            description = """
                    로그인한 유저의 포인트 거래 내역을 최신순으로 커서 기반 스크롤 조회합니다.

                    거래 타입(type)
                    - CHARGE: 충전
                    - WITHDRAW: 출금
                    - DEPOSIT: 예치
                    - REFUND: 환급 (예치금 + 보너스/차감 조정액 포함)

                    type) RERUND일 때만 depositAmount(예치금), adjustmentAmount(조정금-보너스지급/차감) 값 반환
                    """
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "timestamp": "2026-07-24T15:28:37.047Z",
                          "code": "SUCCESS",
                          "message": "요청에 성공하였습니다.",
                          "result": {
                            "pointTransactions": [
                              {
                                "type": "REFUND",
                                "title": "2챌린지 실패",
                                "amount": 3500,
                                "depositAmount": 3000,
                                "adjustmentAmount": 500,
                                "balanceAfter": 10000,
                                "date": "2026-07-24"
                              },
                              {
                                "type": "REFUND",
                                "title": "1챌린지 실패",
                                "amount": 1500,
                                "depositAmount": 2000,
                                "adjustmentAmount": -500,
                                "balanceAfter": 6500,
                                "date": "2026-07-24"
                              },
                              {
                                "type": "DEPOSIT",
                                "title": "2챌린지 예치",
                                "amount": -3000,
                                "depositAmount": null,
                                "adjustmentAmount": null,
                                "balanceAfter": 5000,
                                "date": "2026-07-19"
                              }
                            ],
                            "nextCursor": 2,
                            "hasNext": true
                          }
                        }
                        """
                    )
            )
    )
    BaseResponse<PointTransactionScrollResDto> getPointTransactions(
            @AuthUser
            Long userId,

            @Parameter(description = "거래 타입 필터. 미입력 시 전체 조회", example = "REFUND")
            @RequestParam(required = false)
            PointTransactionType type,

            @Parameter(description = "이전 응답의 nextCursor 값. 첫 조회 시 미입력", example = "100")
            @RequestParam(required = false)
            Long cursor,

            @Parameter(description = "조회할 개수. 기본값 10, 최소 1", example = "10")
            @Min(1)
            @RequestParam(defaultValue = "10")
            int size
    );
}
