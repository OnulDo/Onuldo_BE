package com.example.onuldo.domain.challenge.scheduler;

import com.example.onuldo.domain.challenge.service.ChallengeParticipantCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeParticipantCountScheduler {

    private final ChallengeParticipantCountService challengeParticipantCountService;

    @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
    public void updateParticipantCounts() {
        int updatedChallengeCount = challengeParticipantCountService.updateParticipantCounts();
        log.info("updateParticipantCounts finished. updatedChallengeCount={}", updatedChallengeCount);
    }
}
