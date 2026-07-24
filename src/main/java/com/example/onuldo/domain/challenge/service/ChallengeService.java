package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.response.ChallengePageResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;

    public ChallengePageResDto getChallenges(
            int page,
            int size,
            ChallengeCategory category,
            String s
    ) {
        Page<Challenge> challengePage = challengeRepository.findChallenges(
                ChallengeStatus.ACTIVE,
                category,
                normalizeKeyword(s),
                PageRequest.of(page, resolveSize(size))
        );

        return ChallengePageResDto.builder()
                .challenges(challengePage.getContent().stream().map(this::toChallengeResDto).toList())
                .page(challengePage.getNumber())
                .size(challengePage.getSize())
                .totalElements(challengePage.getTotalElements())
                .totalPages(challengePage.getTotalPages())
                .hasNext(challengePage.hasNext())
                .build();
    }

    public ChallengeResDto getChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        return toChallengeResDto(challenge);
    }

    private ChallengeResDto toChallengeResDto(Challenge challenge) {
        return ChallengeResDto.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .explainContent(challenge.getExplainContent())
                .captionImgUrl(challenge.getCaptionImgUrl())
                .participantCount(challenge.getParticipantCount())
                .category(challenge.getCategory())
                .timeStart(challenge.getTimeStart())
                .timeEnd(challenge.getTimeEnd())
                .durationOptionList(challenge.getDurationOptionList())
                .depositOptionList(challenge.getDepositOptionList())
                .successConditionList(challenge.getSuccessConditionList())
                .failureConditionList(challenge.getFailureConditionList())
                .verificationLabelList(challenge.getVerificationLabelList())
                .build();
    }

    private String normalizeKeyword(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private int resolveSize(int size) {
        return Math.min(size, 50);
    }
}
