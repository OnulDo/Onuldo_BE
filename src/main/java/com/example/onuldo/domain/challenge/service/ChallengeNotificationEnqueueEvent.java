package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.user.service.NotificationDispatchCommand;

public record ChallengeNotificationEnqueueEvent(NotificationDispatchCommand command) {
}
