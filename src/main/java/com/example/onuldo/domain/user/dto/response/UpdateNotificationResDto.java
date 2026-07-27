package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UpdateNotificationResDto (
        @Schema(example = "CHALLENGE_START")
        NotificationType type,
        @Schema(example = "false")
        Boolean enabled
){

}
