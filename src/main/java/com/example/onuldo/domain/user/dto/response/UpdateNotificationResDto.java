package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.NotificationSettingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UpdateNotificationResDto (
        @Schema(example = "CHALLENGE_START")
        NotificationSettingType type,
        @Schema(example = "false")
        Boolean enabled
){

}
