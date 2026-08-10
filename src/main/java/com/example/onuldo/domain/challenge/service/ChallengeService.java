package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.response.ChallengeManualReviewResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeVerificationResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.challenge.dto.request.ChallengeVerificationReqDto;
import com.example.onuldo.global.aws.service.RekognitionService;
import com.example.onuldo.global.aws.service.S3FileService;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementService settlementService;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;
    private final RekognitionService rekognitionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3FileService s3FileService;

    public CursorPageResponse<ChallengeResDto> getChallenges(
            String cursor,
            int size,
            ChallengeCategory category,
            String keyword
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);

        Integer lastParticipantCount = null;
        Long lastId = null;
        if (!CursorKeyCodec.isBlank(cursor)) {
            long[] parts = CursorKeyCodec.decodeAsLongs(cursor, 2);
            lastParticipantCount = CursorKeyCodec.toIntCursorValue(parts[0]);
            lastId = parts[1];
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

    @Transactional
    public ChallengeVerificationResDto verifyChallenge(Long userId, Long challengeId, ChallengeVerificationReqDto request) {
        LocalDateTime nowKst = timeService.nowKst();
        LocalDate today = nowKst.toLocalDate();

        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        Participation participation = participationRepository
                .findTopByUser_IdAndChallenge_IdAndStatusOrderByIdDesc(userId, challengeId, ParticipationStatus.ONGOING)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTICIPATION_NOT_FOUND));

        if (today.isBefore(participation.getStartDate())) {
            throw new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_STARTED);
        }

        validateVerificationAvailableTime(challenge, participation, today, nowKst.toLocalTime());

        if (verificationRepository.existsByPhotoUrl(s3FileService.getFileUrl(request.fileId()).url())) {
            throw new RestApiException(GlobalErrorStatus._DUPLICATE_VERIFICATION_PHOTO);
        }

        List<String> detectedLabelNames = rekognitionService.detectLabelNamesByFileId(request.fileId());

        boolean matchedChallengeLabel = hasMatchingLabel(challenge.getVerificationLabelList(), detectedLabelNames);
        VerificationReviewStatus review = matchedChallengeLabel
                ? VerificationReviewStatus.PASS
                : VerificationReviewStatus.AUTO_FAIL;

        BigDecimal dayScore = matchedChallengeLabel ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        String rekognitionResult = toJson(detectedLabelNames);

        Verification verification;
        try {
            verification = verificationRepository.saveAndFlush(Verification.builder()
                    .participation(participation)
                    .verificationDate(today)
                    .photoUrl(s3FileService.getFileUrl(request.fileId()).url())
                    .rekognitionResult(rekognitionResult)
                    .review(review)
                    .dayScore(dayScore)
                    .verifiedAt(timeService.nowKst())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_VERIFIED_TODAY);
        }

        // 인증 성공 시 정산 트리거 호출
        if (verification.getReview() == VerificationReviewStatus.PASS) {
            challengeNotificationService.notifyPartyMemberVerified(verification);
            triggerSettlementIfLastDay(participation, today);
        }

        return ChallengeVerificationResDto.builder()
                .verificationId(verification.getId())
                .challengeId(challenge.getId())
                .participationId(participation.getId())
                .fileId(request.fileId())
                .verificationDate(verification.getVerificationDate())
                .verifiedAt(verification.getVerifiedAt())
                .review(verification.getReview())
                .build();
    }

    private void validateVerificationAvailableTime(
            Challenge challenge,
            Participation participation,
            LocalDate today,
            LocalTime currentTime
    ) {
        if (participation.getEndDate() != null && today.isAfter(participation.getEndDate())) {
            throw new RestApiException(GlobalErrorStatus._CHALLENGE_PARTICIPATION_ENDED);
        }

        if (!ChallengeVerificationTimePolicy.isWithinVerificationTime(
                challenge.getTimeStart(),
                challenge.getTimeEnd(),
                currentTime
        )) {
            throw new RestApiException(GlobalErrorStatus._CHALLENGE_VERIFICATION_TIME_UNAVAILABLE);
        }
    }

    @Transactional
    public ChallengeManualReviewResDto manualReviewVerification(Long userId, Long challengeId) {
        challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        Participation participation = participationRepository
                .findTopByUser_IdAndChallenge_IdAndStatusOrderByIdDesc(userId, challengeId, ParticipationStatus.ONGOING)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTICIPATION_NOT_FOUND));

        LocalDate today = timeService.todayKst();

        if (verificationRepository.findByParticipation_IdAndVerificationDateAndReview(
                participation.getId(), today, VerificationReviewStatus.MANUAL_REVIEW).isPresent()) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_MANUALLY_REVIEWED);
        }

        if (verificationRepository.findByParticipation_IdAndVerificationDateAndReview(
                participation.getId(), today, VerificationReviewStatus.PASS).isPresent()) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_VERIFIED_TODAY);
        }

        Verification autoFail = verificationRepository.findByParticipation_IdAndVerificationDateAndReview(
                        participation.getId(), today, VerificationReviewStatus.AUTO_FAIL)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._AUTO_FAIL_VERIFICATION_NOT_FOUND));

        Verification manualReview;
        try{
            manualReview = verificationRepository.save(Verification.builder()
                    .participation(participation)
                    .originalVerification(autoFail)
                    .verificationDate(autoFail.getVerificationDate())
                    .photoUrl(null)
                    .exifData(autoFail.getExifData())
                    .rekognitionResult(autoFail.getRekognitionResult())
                    .aiScore(autoFail.getAiScore())
                    .review(VerificationReviewStatus.MANUAL_REVIEW)
                    .dayScore(autoFail.getDayScore())
                    .verifiedAt(timeService.nowKst())
                    .build());
        } catch(DataIntegrityViolationException e) {
            log.warn("Manual review insert failed with constraint violation. participationId={}, date={}, message={}",
                    participation.getId(), today, e.getMostSpecificCause().getMessage(), e);
            // 동시 요청 경합으로 먼저 들어온 쪽이 이미 만든 row가 있으면 그걸 그대로 성공 응답으로 처리
            manualReview = verificationRepository.findByParticipation_IdAndVerificationDateAndReview(
                            participation.getId(), today, VerificationReviewStatus.MANUAL_REVIEW)
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._ALREADY_MANUALLY_REVIEWED));
        }


        return ChallengeManualReviewResDto.builder()
                .manualReviewRequestedAt(manualReview.getVerifiedAt())
                .build();
    }

    private void triggerSettlementIfLastDay(Participation participation, LocalDate verificationDate) {
        if (!verificationDate.equals(participation.getEndDate())) {
            return;
        }

        settlementService.settleParticipatedChallenge(participation.getId());
    }

    private ChallengeResDto toChallengeResDto(Challenge challenge) {
        return ChallengeResDto.builder()
                .challengeId(challenge.getId())
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

    private boolean hasMatchingLabel(List<String> challengeLabels, List<String> detectedLabels) {
        if(challengeLabels == null || challengeLabels.isEmpty()) {
            return false;
        }
        if (detectedLabels == null || detectedLabels.isEmpty()) {
            return false;
        }

        return challengeLabels.stream()
                .filter(label -> label != null && !label.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(normalizedChallengeLabel ->
                        detectedLabels.stream()
                                .filter(label -> label != null && !label.isBlank())
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .anyMatch(normalizedChallengeLabel::equals)
                );
    }

    private String toJson(List<String> detectedLabelNames) {
        try {
            return objectMapper.writeValueAsString(detectedLabelNames);
        } catch (JsonProcessingException e) {
            throw new RestApiException(GlobalErrorStatus._VERIFICATION_RESULT_SERIALIZATION_FAILED);
        }
    }
}
