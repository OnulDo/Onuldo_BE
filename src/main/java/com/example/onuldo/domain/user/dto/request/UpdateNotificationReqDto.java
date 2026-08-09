package com.example.onuldo.domain.user.dto.request;

import com.example.onuldo.domain.user.enums.NotificationSettingType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateNotificationReqDto (
        @NotNull
        NotificationSettingType type,
        @NotNull
        Boolean enabled
){

}
