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

    private static final int MAX_PAGE_SIZE = 50;

    private final ChallengeRepository challengeRepository;

    public ChallengePageResDto getChallenges(
            int page,
            int size,
            ChallengeCategory category,
            String keyword
    ) {
        validatePage(page);
        int resolvedSize = getResolvedSize(size);

        Page<Challenge> challengePage = challengeRepository.findChallenges(
                ChallengeStatus.ACTIVE,
                category,
                normalizeKeyword(keyword),
                PageRequest.of(page, resolvedSize)
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
                .description(challenge.getDescription())
                .captionImgUrl(challenge.getCaptionImgUrl())
                .verifyMethodContent(challenge.getVerifyMethodContent())
                .verificationExamplePhotoUrl(challenge.getVerificationExamplePhotoUrl())
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

    private int getResolvedSize(int size) {
        if (size <= 0) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "페이지 크기는 1 이상이어야 합니다.");
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }
    }
}
