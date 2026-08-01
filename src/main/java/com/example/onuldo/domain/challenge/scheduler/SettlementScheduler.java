package com.example.onuldo.domain.challenge.scheduler;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.service.SettlementService;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final ParticipationRepository participationRepository;
    private final SettlementService settlementService;
    private final TimeService timeService;

    @Scheduled(cron = "0 */10 * * * *")
    public void patchFailedSettlement() {
        var nowUtc = timeService.nowUtc();
        var nowKst = timeService.nowKst();

        log.info("patchFailedSettlement started. utcNow={}, kstNow={}", nowUtc, nowKst);

        List<Long> targetParticipationIds = participationRepository.findFailedSettlementTargets(
                ParticipationStatus.ONGOING,
                timeService.todayKst(),
                nowKst.toLocalTime()
        ).stream()
                .map(Participation::getId)
                .toList();

        for (Long participationId : targetParticipationIds) {
            settlementService.settleParticipatedChallenge(participationId);
        }

        log.info("patchFailedSettlement finished. targetCount={}", targetParticipationIds.size());

    }
}
