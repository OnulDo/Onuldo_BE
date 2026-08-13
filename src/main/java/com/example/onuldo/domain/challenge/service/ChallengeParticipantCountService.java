package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.repository.ChallengeParticipantCountProjection;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeParticipantCountService {

    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public int updateParticipantCounts() {
        Map<Long, Integer> countByChallengeId = participationRepository
                .countByChallengeIdExcludingStatus(ParticipationStatus.CANCELED)
                .stream()
                .collect(Collectors.toMap(
                        ChallengeParticipantCountProjection::challengeId,
                        projection -> Math.toIntExact(projection.count())
                ));

        var challenges = challengeRepository.findAll();
        challenges.forEach(challenge -> updateParticipantCount(challenge, countByChallengeId));

        return challenges.size();
    }

    private void updateParticipantCount(Challenge challenge, Map<Long, Integer> countByChallengeId) {
        challenge.updateParticipantCount(countByChallengeId.getOrDefault(challenge.getId(), 0));
    }
}
