package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.user.enums.NotificationType;
import lombok.Builder;

@Builder
public record UpdateNotificationResDto (
        NotificationType type,
        Boolean enabled
){

}