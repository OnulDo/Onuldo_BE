package com.example.onuldo.domain.user.scheduler;

import com.example.onuldo.domain.user.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchScheduler {

    private final NotificationDispatchService notificationDispatchService;

    @Scheduled(cron = "0 * * * * *")
    public void dispatchNotifications() {
        var targetIds = notificationDispatchService.sendDueDispatches();
        log.info("dispatchNotifications finished. targetCount={}", targetIds.size());
    }
}
