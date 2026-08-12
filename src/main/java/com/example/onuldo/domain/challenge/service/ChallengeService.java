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
import com.example.onuldo.global.common.exception.BusinessRuleException;
import com.example.onuldo.global.common.exception.DuplicateException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private static final String PHOTO_URL_UNIQUE_CONSTRAINT = "uk_verification_photo_url";

    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementService settlementService;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;
    private final RekognitionService rekognitionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3FileService s3FileService;
    private final PlatformTransactionManager transactionManager;

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
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));

        return toChallengeResDto(challenge);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChallengeVerificationResDto verifyChallenge(Long userId, Long challengeId, ChallengeVerificationReqDto request) {
        LocalDateTime nowKst = timeService.nowKst();
        LocalDate today = nowKst.toLocalDate();

        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));

        Participation participationSnapshot = participationRepository
                .findLatestByUserIdAndChallengeIdAndStatus(userId, challengeId, ParticipationStatus.ONGOING)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTICIPATION_NOT_FOUND));

        if (today.isBefore(participationSnapshot.getStartDate())) {
            throw new BusinessRuleException(ErrorStatus._CHALLENGE_NOT_STARTED);
        }

        validateVerificationAvailableTime(challenge, participationSnapshot, today, nowKst.toLocalTime());

        if (verificationRepository.existsByParticipation_IdAndVerificationDateAndReview(
                participationSnapshot.getId(), today, VerificationReviewStatus.PASS)) {
            throw new DuplicateException(ErrorStatus._ALREADY_VERIFIED_TODAY);
        }

        String photoUrl = s3FileService.getFileUrl(request.fileId()).url();
        if (verificationRepository.existsByPhotoUrl(photoUrl)) {
            throw new DuplicateException(ErrorStatus._DUPLICATE_VERIFICATION_PHOTO);
        }

        List<String> detectedLabelNames = rekognitionService.detectLabelNamesByFileId(request.fileId());

        boolean matchedChallengeLabel = hasMatchingLabel(challenge.getVerificationLabelList(), detectedLabelNames);
        VerificationReviewStatus review = matchedChallengeLabel
                ? VerificationReviewStatus.PASS
                : VerificationReviewStatus.AUTO_FAIL;

        BigDecimal dayScore = matchedChallengeLabel ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        String rekognitionResult = toJson(detectedLabelNames);

        VerificationWriteResult saved = executeInTransaction(status -> saveVerificationInTransaction(
                userId,
                challengeId,
                today,
                nowKst.toLocalTime(),
                photoUrl,
                rekognitionResult,
                review,
                dayScore
        ));

        if (saved.review() == VerificationReviewStatus.PASS) {
            challengeNotificationService.notifyPartyMemberVerified(saved.verificationId());
            triggerSettlementIfLastDay(saved.participationId(), saved.participationEndDate(), today);
        }

        return ChallengeVerificationResDto.builder()
                .verificationId(saved.verificationId())
                .challengeId(challengeId)
                .participationId(saved.participationId())
                .fileId(request.fileId())
                .verificationDate(saved.verificationDate())
                .verifiedAt(saved.verifiedAt())
                .review(saved.review())
                .build();
    }

    private VerificationWriteResult saveVerificationInTransaction(
            Long userId,
            Long challengeId,
            LocalDate today,
            LocalTime currentTime,
            String photoUrl,
            String rekognitionResult,
            VerificationReviewStatus review,
            BigDecimal dayScore
    ) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));

        Participation participation = participationRepository
                .findLatestByUserIdAndChallengeIdAndStatusForUpdate(userId, challengeId, ParticipationStatus.ONGOING)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTICIPATION_NOT_FOUND));

        if (today.isBefore(participation.getStartDate())) {
            throw new BusinessRuleException(ErrorStatus._CHALLENGE_NOT_STARTED);
        }

        validateVerificationAvailableTime(challenge, participation, today, currentTime);

        if (verificationRepository.existsByParticipation_IdAndVerificationDateAndReview(
                participation.getId(), today, VerificationReviewStatus.PASS)) {
            throw new DuplicateException(ErrorStatus._ALREADY_VERIFIED_TODAY);
        }

        if (verificationRepository.existsByPhotoUrl(photoUrl)) {
            throw new DuplicateException(ErrorStatus._DUPLICATE_VERIFICATION_PHOTO);
        }

        Verification verification;
        try {
            verification = verificationRepository.saveAndFlush(Verification.builder()
                    .participation(participation)
                    .verificationDate(today)
                    .photoUrl(photoUrl)
                    .rekognitionResult(rekognitionResult)
                    .review(review)
                    .dayScore(dayScore)
                    .verifiedAt(timeService.nowKst())
                    .build());
        } catch (DataIntegrityViolationException e) {
            if (isPhotoUrlUniqueViolation(e)) {
                throw new DuplicateException(ErrorStatus._DUPLICATE_VERIFICATION_PHOTO);
            }
            throw new InternalServerException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }

        return new VerificationWriteResult(
                verification.getId(),
                participation.getId(),
                participation.getEndDate(),
                verification.getVerificationDate(),
                verification.getVerifiedAt(),
                verification.getReview()
        );
    }

    private void validateVerificationAvailableTime(
            Challenge challenge,
            Participation participation,
            LocalDate today,
            LocalTime currentTime
    ) {
        if (participation.getEndDate() != null && today.isAfter(participation.getEndDate())) {
            throw new BusinessRuleException(ErrorStatus._CHALLENGE_PARTICIPATION_ENDED);
        }

        if (!ChallengeVerificationTimePolicy.isWithinVerificationTime(
                challenge.getTimeStart(),
                challenge.getTimeEnd(),
                currentTime
        )) {
            throw new BusinessRuleException(ErrorStatus._CHALLENGE_VERIFICATION_TIME_UNAVAILABLE);
        }
    }

    private boolean isPhotoUrlUniqueViolation(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException cve) {
                return PHOTO_URL_UNIQUE_CONSTRAINT.equals(cve.getConstraintName());
            }
        }
        return false;
    }

    @Transactional
    public ChallengeManualReviewResDto manualReviewVerification(Long userId, Long challengeId) {
        challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));

        Participation participation = participationRepository
                .findLatestByUserIdAndChallengeIdAndStatusForUpdate(userId, challengeId, ParticipationStatus.ONGOING)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTICIPATION_NOT_FOUND));

        LocalDate today = timeService.todayKst();

        Optional<Verification> existingManualReview = verificationRepository
                .findFirstByParticipation_IdAndVerificationDateAndReviewOrderByVerifiedAtDescIdDesc(
                        participation.getId(), today, VerificationReviewStatus.MANUAL_REVIEW);
        if (existingManualReview.isPresent()) {
            // 이미 직접 검토 요청이 접수된 상태이므로, 재요청도 동일 결과로 idempotent하게 성공 처리
            return ChallengeManualReviewResDto.builder()
                    .manualReviewRequestedAt(existingManualReview.get().getVerifiedAt())
                    .build();
        }

        if (verificationRepository.existsByParticipation_IdAndVerificationDateAndReview(
                participation.getId(), today, VerificationReviewStatus.PASS)) {
            throw new DuplicateException(ErrorStatus._ALREADY_VERIFIED_TODAY);
        }

        Verification autoFail = verificationRepository.findFirstByParticipation_IdAndVerificationDateAndReviewOrderByVerifiedAtDescIdDesc(
                        participation.getId(), today, VerificationReviewStatus.AUTO_FAIL)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._AUTO_FAIL_VERIFICATION_NOT_FOUND));

        Verification manualReview = verificationRepository.save(Verification.builder()
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

        return ChallengeManualReviewResDto.builder()
                .manualReviewRequestedAt(manualReview.getVerifiedAt())
                .build();
    }

    private <T> T executeInTransaction(TransactionCallback<T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(callback);
    }

    private void triggerSettlementIfLastDay(Long participationId, LocalDate participationEndDate, LocalDate verificationDate) {
        if (!verificationDate.equals(participationEndDate)) {
            return;
        }

        settlementService.settleParticipatedChallenge(participationId);
    }

    private record VerificationWriteResult(
            Long verificationId,
            Long participationId,
            LocalDate participationEndDate,
            LocalDate verificationDate,
            LocalDateTime verifiedAt,
            VerificationReviewStatus review
    ) {
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
            throw new InternalServerException(ErrorStatus._VERIFICATION_RESULT_SERIALIZATION_FAILED);
        }
    }
}
