package com.example.onuldo.domain.challenge.scheduler;

import com.example.onuldo.domain.challenge.service.ChallengePotRateService;
import com.example.onuldo.global.common.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengePotRateScheduler {

    private final ChallengePotRateService challengePotRateService;

    // 매주 월요일 04:00 KST — 자립유지식 β 재계산 (ChallengePotRateService 참고)
    // JVM 기본 타임존이 KST가 아닐 수 있어 zone을 명시적으로 고정한다.
    @Scheduled(cron = "0 0 4 * * MON", zone = "Asia/Seoul")
    public void recalculateBonusRate() {
        try {
            challengePotRateService.recalculateBonusRate();
        } catch (RestApiException e) {
            log.warn("Failed to recalculate challenge pot bonus rate. message={}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while recalculating challenge pot bonus rate.", e);
        }
    }
}
