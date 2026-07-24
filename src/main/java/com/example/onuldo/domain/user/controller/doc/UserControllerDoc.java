package com.example.onuldo.domain.user.controller.doc;

import com.example.onuldo.domain.user.dto.request.UpdateNotificationReqDto;
import com.example.onuldo.domain.user.dto.response.GetNotificationResDto;
import com.example.onuldo.domain.user.dto.response.UpdateNotificationResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

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
}
