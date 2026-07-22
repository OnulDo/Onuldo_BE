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
            description = "로그인한 유저의 특정 알림 타입 on/off 여부를 변경합니다."
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
