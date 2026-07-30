package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;

    public CursorPageResponse<ChallengeResDto> getChallenges(
            String cursor,
            int size,
            ChallengeCategory category,
            String keyword
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);

        Integer lastParticipantCount = null;
        Long lastId = null;
        if (cursor != null) {
            String[] parts = CursorKeyCodec.decode(cursor);
            lastParticipantCount = Integer.parseInt(parts[0]);
            lastId = Long.parseLong(parts[1]);
        }

        List<Challenge> challenges = challengeRepository.findChallenges(
                ChallengeStatus.ACTIVE,
                category,
                normalizeKeyword(keyword),
                lastParticipantCount,
                lastId,
                CursorPageable.of(resolvedSize)
        );

        return CursorPageResponse.of(
                challenges,
                resolvedSize,
                this::toChallengeResDto,
                c -> CursorKeyCodec.encode(c.getParticipantCount(), c.getId())
        );
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

}
