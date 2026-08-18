package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.user.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 푸시 발송 예약과 알림함 생성을 함께 요청할 때 사용하는 값 객체
@Getter
@Builder
public class NotificationDispatchCommand {

    private final Long userId;
    private final NotificationType type;
    private final String title;
    private final String content;
    private final LocalDateTime scheduledAt;
    private final String refType;
    private final Long refId;
}
