package com.example.onuldo.domain.user.dto.request;

import com.example.onuldo.domain.user.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateNotificationReqDto (
        @NotNull
        NotificationType type,
        @NotNull
        Boolean enabled
){

}
