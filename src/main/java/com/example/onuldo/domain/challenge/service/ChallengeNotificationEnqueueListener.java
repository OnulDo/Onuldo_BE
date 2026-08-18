package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.user.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeNotificationEnqueueListener {

    private final NotificationDispatchService notificationDispatchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enqueueAfterCommit(ChallengeNotificationEnqueueEvent event) {
        try {
            notificationDispatchService.enqueue(event.command());
        } catch (Exception e) {
            log.warn("Failed to enqueue challenge notification after transaction commit. command={}", event.command(), e);
        }
    }
}
