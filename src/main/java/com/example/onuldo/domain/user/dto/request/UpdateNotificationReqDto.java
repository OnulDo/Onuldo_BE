package com.example.onuldo.domain.user.dto.request;

import com.example.onuldo.domain.user.enums.NotificationSettingType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateNotificationReqDto (
        @NotNull(message = "알림 타입은 필수입니다.")
        NotificationSettingType type,
        @NotNull(message = "알림 활성화 여부는 필수입니다.")
        Boolean enabled
){

}
