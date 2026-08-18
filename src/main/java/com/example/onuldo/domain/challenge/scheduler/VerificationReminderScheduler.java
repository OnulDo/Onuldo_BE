package com.example.onuldo.domain.challenge.scheduler;

import com.example.onuldo.domain.challenge.service.ChallengeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationReminderScheduler {

    private final ChallengeNotificationService challengeNotificationService;

    // 매분 인증 마감 리마인더 발송 대상을 확인하는 스케줄러 메서드
    @Scheduled(cron = "0 * * * * *")
    public void sendVerificationDeadlineReminders() {
        challengeNotificationService.sendDueVerificationDeadlineReminders();
        log.info("sendVerificationDeadlineReminders finished.");
    }
}
