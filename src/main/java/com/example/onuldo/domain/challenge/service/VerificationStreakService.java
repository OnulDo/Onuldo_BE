package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationStreakService {

    private final VerificationRepository verificationRepository;

    public Map<Long, Integer> calculateStreaks(Collection<Long> participationIds, LocalDate date) {
        if (participationIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<LocalDate>> passDatesByParticipationId = verificationRepository
                .findAllByParticipation_IdIn(participationIds)
                .stream()
                .filter(verification -> verification.getReview() == VerificationReviewStatus.PASS)
                .collect(
                        Collectors.groupingBy(
                    verification -> verification.getParticipation().getId(),
                    Collectors.mapping(Verification::getVerificationDate, Collectors.toSet())
            )
        );

        Map<Long, Integer> streakByParticipationId = new HashMap<>();
        for (Long participationId : participationIds) {
            List<LocalDate> passDates = passDatesByParticipationId
                    .getOrDefault(participationId, Set.of())
                    .stream()
                    .sorted(Comparator.reverseOrder())
                    .toList();

            int streak = 0;
            LocalDate expected = date;
            for (LocalDate passDate : passDates) {
                if (passDate.isAfter(expected)) {
                    continue;
                }
                if (!passDate.equals(expected)) {
                    break;
                }
                streak++;
                expected = expected.minusDays(1);
            }
            streakByParticipationId.put(participationId, streak);
        }
        return streakByParticipationId;
    }
}
