package com.example.onuldo.domain.user.controller.doc;

import com.example.onuldo.domain.user.dto.request.ChargePointReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.request.UpdateProfileReqDto;
import com.example.onuldo.domain.user.dto.request.WithdrawPointReqDto;
import com.example.onuldo.domain.user.dto.response.ChargePointResDto;
import com.example.onuldo.domain.user.dto.response.GetMyPageResDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.GetProfileResDto;
import com.example.onuldo.domain.user.dto.response.NotificationListItemResDto;
import com.example.onuldo.domain.user.dto.response.PointTransactionResDto;
import com.example.onuldo.domain.user.dto.response.PointWalletSummaryResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateProfileResDto;
import com.example.onuldo.domain.user.dto.response.WithdrawPointResDto;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "마이페이지 API")
public interface UserControllerDoc {

    @Operation(
            summary = "프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    BaseResponse<GetProfileResDto> getProfile(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "마이페이지 메인 조회",
            description = "현재 로그인한 사용자의 마이페이지 메인 정보를 조회합니다."
    )
    BaseResponse<GetMyPageResDto> getMyPage(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "프로필 사진/닉네임 변경",
            description = """
                    로그인한 유저의 프로필 정보(사진, 닉네임)를 변경합니다.

                    nickname, profileImageUrl 중 변경할 값만 채워서 요청하면 됩니다.
                    - 값을 채운 필드만 변경되고, null로 보낸 필드는 기존 값이 유지됩니다.
                    - 두 필드를 모두 채워 보내면 둘 다 한 번에 변경됩니다.
                    """
    )
    BaseResponse<UpdateProfileResDto> updateProfile(
            @AuthUser
            Long userId,
            @RequestBody
            UpdateProfileReqDto request
    );

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
            content = @Content(mediaType = "application/json")
    )
    BaseResponse<UpdateNotificationResDto> updateNotification(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            UpdateNotificationReqDto request
    );

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    로그인한 유저의 알림함 목록을 최신순 커서 기반 스크롤로 조회합니다.

                    응답에는 알림 카드 화면 구성을 위한 제목, 본문, 상대 시간, 읽음 여부와 이동에 필요한 challengeId/partyId가 포함됩니다.
                    알림 보관 기간은 30일이며, 생성 후 30일이 지난 알림은 조회되지 않습니다.
                    """
    )
    @ApiResponse(responseCode = "200")
    CursorPageResponse<NotificationListItemResDto> getNotifications(
            @AuthUser
            Long userId,

            @Parameter(description = "이전 응답의 nextCursor 값. 첫 조회 시 미입력")
            @RequestParam(required = false)
            String cursor,

            @Parameter(description = "조회할 개수. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size
    );

    @Operation(
            summary = "포인트 충전",
            description = "포인트를 충전하고 충전된 금액과 충전 후 잔액을 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<ChargePointResDto> chargePoint(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            ChargePointReqDto request
    );

    @Operation(
            summary = "포인트 출금",
            description = "포인트를 출금하고 출금된 금액과 출금 후 잔액을 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json")
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<WithdrawPointResDto> withdrawPoint(
            @AuthUser
            Long userId,

            @Valid
            @RequestBody
            WithdrawPointReqDto request
    );

    @Operation(
            summary = "신규 회원 가입 포인트 지급",
            description = """
                    회원가입 직후 클라이언트가 호출하면 서버가 신규 회원 가입 포인트 100,000P를 지급하고,
                    지급된 금액과 지급 후 잔액을 반환합니다.
                    """
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<ChargePointResDto> grantSignupBonus(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "포인트 지갑 요약 조회",
            description = "잔액, 예치 중인 포인트, 누적 예치액, 누적 환급액, 누적 차감액, 평균 환급률을 조회합니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<PointWalletSummaryResDto> getPointWalletSummary(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다. 탈퇴 후에는 기존 토큰으로 API 접근이 차단됩니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<Void> withdraw(
            @AuthUser
            Long userId
    );

    @Operation(
            summary = "포인트 거래 내역 조회",
            description = """
                    로그인한 유저의 포인트 거래 내역을 최신순으로 커서 기반 스크롤 조회합니다.

                    거래 타입(type)
                    - CHARGE: 충전 (신규 회원 가입 포인트 지급 포함)
                    - WITHDRAW: 출금
                    - DEPOSIT: 예치
                    - REFUND: 환급 (예치금 + 보너스/차감 조정액 포함)

                    type) RERUND일 때만 depositAmount(예치금), adjustmentAmount(조정금-보너스지급/차감) 값 반환
                    """
    )
    @ApiResponse(responseCode = "200")
    CursorPageResponse<PointTransactionResDto> getPointTransactions(
            @AuthUser
            Long userId,

            @Parameter(description = "거래 타입 필터. 미입력 시 전체 조회", example = "REFUND")
            @RequestParam(required = false)
            PointTransactionType type,

            @Parameter(description = "이전 응답의 nextCursor 값. 첫 조회 시 미입력")
            @RequestParam(required = false)
            String cursor,

            @Parameter(description = "조회할 개수. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size
    );
}
