package com.example.onuldo.domain.challenge.scheduler;

import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.service.PartySettlementService;
import com.example.onuldo.domain.challenge.service.SettlementService;
import com.example.onuldo.global.common.exception.RestApiException;
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
    private final PartySettlementService partySettlementService;
    private final TimeService timeService;

    @Scheduled(cron = "0 */10 * * * *")
    public void patchFailedSettlement() {
        var nowUtc = timeService.nowUtc();
        var nowKst = timeService.nowKst();

        log.info("patchFailedSettlement started. utcNow={}, kstNow={}", nowUtc, nowKst);

        List<Long> targetParticipationIds = participationRepository.findFailedSettlementTargets(
                ParticipationStatus.ONGOING,
                nowKst.toLocalDate(),
                nowKst.toLocalTime()
        ).stream()
                .map(Participation::getId)
                .toList();

        for (Long participationId : targetParticipationIds) {
            try {
                settlementService.settleParticipatedChallenge(participationId);
            } catch (RestApiException e) {
                log.warn("Failed to settle participation. participationId={}, message={}", participationId, e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error while settling participation. participationId={}", participationId, e);
            }
        }

        log.info("patchFailedSettlement finished. targetCount={}", targetParticipationIds.size());

    }

    // POI-08: 마지막 수행일 인증 마감이 지난 파티를 파티 단위로 1회 정산한다.
    // (개인 실패 스윕은 마지막 날 PASS 한 파티원을 제외하므로 전원 성공 파티를 놓친다.)
    @Scheduled(cron = "0 */10 * * * *")
    public void patchPartySettlement() {
        var nowKst = timeService.nowKst();

        log.info("patchPartySettlement started. kstNow={}", nowKst);

        List<Long> targetPartyIds = participationRepository.findPartySettlementTargetPartyIds(
                ParticipationStatus.ONGOING,
                nowKst.toLocalDate(),
                nowKst.toLocalTime()
        );

        for (Long partyId : targetPartyIds) {
            try {
                partySettlementService.settleParty(partyId);
            } catch (RestApiException e) {
                log.warn("Failed to settle party. partyId={}, message={}", partyId, e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error while settling party. partyId={}", partyId, e);
            }
        }

        log.info("patchPartySettlement finished. targetCount={}", targetPartyIds.size());
    }
}
