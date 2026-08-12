package com.example.onuldo.domain.challenge.service;

import com.example.onuldo.domain.challenge.dto.request.ParticipationReqDto;
import com.example.onuldo.domain.challenge.dto.response.CompletedChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.CompletedPartyResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.DailyCompletedChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.ParticipationResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.ChallengePot;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ChallengeStatus;
import com.example.onuldo.domain.challenge.enums.DailyChallengeStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ChallengePotRepository;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.PartyCountProjection;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.challenge.support.ParticipationValidator;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.InsufficientPointException;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.InvalidRequestException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengePotRepository challengePotRepository;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;
    private final ParticipationValidator participationValidator;
    private final VerificationStreakService verificationStreakService;

    // 개인 챌린지 몰수금 pot은 싱글톤 행 하나로 운용한다.
    private static final Long CHALLENGE_POT_ID = 1L;
    private final DailyChallengeStatusResolver dailyChallengeStatusResolver;

    public ParticipationResDto participatePersonalChallenge(
            Long userId,
            Long challengeId,
            ParticipationReqDto request
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        Challenge challenge = challengeRepository.findById(challengeId)
                .filter(found -> found.getStatus() == ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));

        challenge.validateDurationOption(request.durationWeeks());
        challenge.validateDepositOption(request.depositAmount());
        participationValidator.validateNotOngoing(userId, challengeId);
        validatePointBalance(user, request.depositAmount());

        // durationWeeks*7일은 시작일·종료일을 포함한 총 수행일수 (ParticipationRecordService.calculateInclusiveDays와 동일 기준)
        LocalDate startDate = timeService.todayKst().plusDays(1);
        LocalDate endDate = startDate.plusWeeks(request.durationWeeks()).minusDays(1);
        Integer durationDays = request.durationWeeks() * 7;

        long balanceAfter = user.getPointBalance() - request.depositAmount();
        user.setPointBalance(balanceAfter);
        userRepository.save(user);

        // 이 순간의 pot 운영 β를 참가에 스냅샷 — 이후 β가 재조정돼도 이 참가는 이 값으로 정산된다.
        ChallengePot pot = challengePotRepository.findByIdForUpdate(CHALLENGE_POT_ID)
                .orElseThrow(() -> new InternalServerException(ErrorStatus._CHALLENGE_POT_NOT_FOUND));
        BigDecimal appliedBonusRate = pot.getCurrentBonusRate();

        Participation participation = createPersonalParticipation(
                user, challenge, request.depositAmount(), request.durationWeeks(), startDate, endDate, appliedBonusRate
        );
        participationRepository.save(participation);
        challengeNotificationService.scheduleChallengeLifecycleNotifications(participation);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.DEPOSIT)
                .amount(-request.depositAmount())
                .balanceAfter(balanceAfter)
                .description(challenge.getName())
                .build()
        );

        int bonusAmount = BigDecimal.valueOf(request.depositAmount())
                .multiply(appliedBonusRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int expectedRefundAmount = request.depositAmount() + bonusAmount;

        return ParticipationResDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .durationWeeks(request.durationWeeks())
                .durationDays(durationDays)
                .depositAmount(request.depositAmount())
                .expectedRefundAmount(expectedRefundAmount)
                .build();
    }

    public CursorPageResponse<UserChallengeResDto> getUserChallenges(
            Long userId,
            ParticipationStatus status,
            String cursor,
            int size
    ) {
        int resolvedSize = CursorConstants.resolveSize(size);

        Long lastId = CursorKeyCodec.isBlank(cursor) ? null : CursorKeyCodec.decodeAsLongs(cursor, 1)[0];

        List<Participation> participations = status == null
                ? participationRepository.findAllByUser_IdOrderByIdDesc(userId, lastId, CursorPageable.of(resolvedSize))
                : participationRepository.findAllByUser_IdAndStatusOrderByIdDesc(userId, status, lastId, CursorPageable.of(resolvedSize));

        return CursorPageResponse.of(
                participations,
                resolvedSize,
                this::toUserChallengeResDto,
                p -> CursorKeyCodec.encode(p.getId())
        );

    }

    public List<DailyChallengeResDto> getDailyChallenges(Long userId) {
        List<Participation> participations = participationRepository
                .findAllWithChallengeByUserIdAndStatusAndParticipationTypeOrderByIdDesc(
                        userId, ParticipationStatus.ONGOING, ParticipationType.PERSONAL
                );
        LocalDateTime now = timeService.nowKst();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        Map<Long, Verification> latestVerificationByParticipationId = findLatestVerificationByParticipationId(
                participations.stream().map(Participation::getId).toList(),
                today
        );
        Map<Long, Integer> streakByParticipationId = verificationStreakService.calculateStreaks(
                participations.stream().map(Participation::getId).toList(),
                today
        );

        return participations.stream()
                .map(participation -> toDailyChallengeResDto(
                        participation,
                        latestVerificationByParticipationId.get(participation.getId()),
                        streakByParticipationId.getOrDefault(participation.getId(), 0),
                        today,
                        currentTime
                ))
                .toList();
    }

    public DailyCompletedChallengeListResDto getDailyCompletedChallenges(Long userId) {
        LocalDate date = timeService.todayKst();

        List<Participation> participations = participationRepository
                .findAllByUser_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
                        userId,
                        ParticipationStatus.ONGOING,
                        date,
                        date
                );

        Map<Long, LocalDateTime> verifiedAtByParticipationId = verificationRepository
                .findVerifiedVerificationsByUserIdAndVerificationDate(userId, date)
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getParticipation().getId(),
                        Verification::getVerifiedAt
                ));

        List<Participation> completed = participations.stream()
                .filter(participation -> verifiedAtByParticipationId.containsKey(participation.getId()))
                .toList();

        Comparator<Participation> byVerifiedAt = Comparator
                .comparing((Participation p) -> verifiedAtByParticipationId.get(p.getId()));

        List<Participation> completedParties = completed.stream()
                .filter(participation -> participation.getParticipationType() == ParticipationType.PARTY)
                .toList();

        List<Long> partyIds = completedParties.stream()
                .map(participation -> participation.getParty().getId())
                .distinct()
                .toList();

        Map<Long, Long> totalMemberCountByPartyId = partyIds.isEmpty()
                ? Map.of()
                : toCountMap(participationRepository.findParticipationCountsByPartyIdInAndStatus(partyIds, ParticipationStatus.ONGOING));
        Map<Long, Long> verifiedMemberCountByPartyId = partyIds.isEmpty()
                ? Map.of()
                : toCountMap(verificationRepository.findAutoPassVerificationCountsByPartyIdInAndVerificationDate(partyIds, date));

        List<CompletedPartyResDto> parties = completedParties.stream()
                .sorted(byVerifiedAt)
                .map(participation -> toCompletedPartyResDto(
                        participation,
                        verifiedAtByParticipationId.get(participation.getId()),
                        totalMemberCountByPartyId,
                        verifiedMemberCountByPartyId
                ))
                .toList();

        List<Participation> completedChallenges = completed.stream()
                .filter(participation -> participation.getParticipationType() == ParticipationType.PERSONAL)
                .toList();

        Map<Long, Integer> streakByParticipationId = verificationStreakService.calculateStreaks(
                completedChallenges.stream().map(Participation::getId).toList(),
                date
        );

        List<CompletedChallengeResDto> challenges = completedChallenges.stream()
                .sorted(byVerifiedAt)
                .map(participation -> toCompletedChallengeResDto(
                        participation,
                        verifiedAtByParticipationId.get(participation.getId()),
                        streakByParticipationId.get(participation.getId())
                ))
                .toList();

        return DailyCompletedChallengeListResDto.builder()
                .parties(parties)
                .challenges(challenges)
                .build();
    }

    private Map<Long, Long> toCountMap(List<PartyCountProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                PartyCountProjection::partyId,
                PartyCountProjection::count
        ));
    }

    private void validatePointBalance(User user, Integer depositAmount) {
        long currentPoint = user.getPointBalance();
        if (depositAmount > currentPoint) {
            throw new InsufficientPointException(
                    ErrorStatus._INSUFFICIENT_POINT_FOR_CHALLENGE,
                    currentPoint,
                    depositAmount
            );
        }
    }

    private Participation createPersonalParticipation(
            User user,
            Challenge challenge,
            Integer depositAmount,
            Integer durationWeeks,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal appliedBonusRate
    ) {
        Participation participation = Participation.builder()
                .user(user)
                .challenge(challenge)
                .party(null)
                .participationType(ParticipationType.PERSONAL)
                .depositAmount(depositAmount)
                .durationWeeks(durationWeeks)
                .startDate(startDate)
                .endDate(endDate)
                .appliedBonusRate(appliedBonusRate)
                .build();
        validateParticipationState(participation);
        return participation;
    }

    private void validateParticipationState(Participation participation) {
        if (participation.getParticipationType() == ParticipationType.PERSONAL && participation.getParty() != null) {
            throw new InvalidRequestException(ErrorStatus._PARTICIPATION_PARTY_NOT_ALLOWED);
        }

        if (participation.getParticipationType() == ParticipationType.PARTY && participation.getParty() == null) {
            throw new InvalidRequestException(ErrorStatus._PARTICIPATION_PARTY_REQUIRED);
        }
    }

    private UserChallengeResDto toUserChallengeResDto(Participation participation) {
        Challenge challenge = participation.getChallenge();

        return UserChallengeResDto.builder()
                .participationId(participation.getId())
                .participationStatus(participation.getStatus())
                .participationType(participation.getParticipationType())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .challengeExplainContent(challenge.getExplainContent())
                .challengeDescription(challenge.getDescription())
                .challengeCaptionImgUrl(challenge.getCaptionImgUrl())
                .challengeVerifyMethodContent(challenge.getVerifyMethodContent())
                .challengeVerificationExamplePhotoUrl(challenge.getVerificationExamplePhotoUrl())
                .participantCount(challenge.getParticipantCount())
                .category(challenge.getCategory())
                .timeStart(challenge.getTimeStart())
                .timeEnd(challenge.getTimeEnd())
                .depositAmount(participation.getDepositAmount())
                .durationWeeks(participation.getDurationWeeks())
                .startDate(participation.getStartDate())
                .endDate(participation.getEndDate())
                .build();
    }

    private DailyChallengeResDto toDailyChallengeResDto(
            Participation participation,
            Verification latestVerification,
            int streakDays,
            LocalDate today,
            LocalTime currentTime
    ) {
        Challenge challenge = participation.getChallenge();
        DailyChallengeStatus dailyStatus = dailyChallengeStatusResolver.resolve(
                participation,
                challenge,
                latestVerification,
                today,
                currentTime
        );

        return DailyChallengeResDto.builder()
                .participationId(participation.getId())
                .participationStatus(participation.getStatus())
                .participationType(participation.getParticipationType())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .challengeExplainContent(challenge.getExplainContent())
                .challengeCaptionImgUrl(challenge.getCaptionImgUrl())
                .challengeVerifyMethodContent(challenge.getVerifyMethodContent())
                .participantCount(challenge.getParticipantCount())
                .category(challenge.getCategory())
                .timeStart(challenge.getTimeStart())
                .timeEnd(challenge.getTimeEnd())
                .depositAmount(participation.getDepositAmount())
                .durationWeeks(participation.getDurationWeeks())
                .startDate(participation.getStartDate())
                .endDate(participation.getEndDate())
                .dailyStatus(dailyStatus)
                .verifiedOnDate(dailyStatus == DailyChallengeStatus.SUCCESS)
                .verifiedAt(latestVerification != null ? latestVerification.getVerifiedAt() : null)
                .streakDays(streakDays)
                .build();
    }

    private Map<Long, Verification> findLatestVerificationByParticipationId(
            List<Long> participationIds,
            LocalDate date
    ) {
        if (participationIds.isEmpty()) {
            return Map.of();
        }

        return verificationRepository
                .findAllByParticipationIdInAndVerificationDateOrderByLatest(participationIds, date)
                .stream()
                .collect(Collectors.toMap(
                        verification -> verification.getParticipation().getId(),
                        verification -> verification,
                        (latest, ignored) -> latest,
                        LinkedHashMap::new
                ));
    }

    private CompletedPartyResDto toCompletedPartyResDto(
            Participation participation,
            LocalDateTime verifiedAt,
            Map<Long, Long> totalMemberCountByPartyId,
            Map<Long, Long> verifiedMemberCountByPartyId
    ) {
        Party party = participation.getParty();
        Challenge challenge = participation.getChallenge();

        int totalMemberCount = totalMemberCountByPartyId.getOrDefault(party.getId(), 0L).intValue();
        int verifiedMemberCount = verifiedMemberCountByPartyId.getOrDefault(party.getId(), 0L).intValue();

        return CompletedPartyResDto.builder()
                .partyId(party.getId())
                .partyName(party.getName())
                .challengeId(challenge.getId())
                .verifiedAt(verifiedAt)
                .totalMemberCount(totalMemberCount)
                .verifiedMemberCount(verifiedMemberCount)
                .build();
    }

    private CompletedChallengeResDto toCompletedChallengeResDto(
            Participation participation, LocalDateTime verifiedAt, int streakDays
    ) {
        Challenge challenge = participation.getChallenge();

        return CompletedChallengeResDto.builder()
                .participationId(participation.getId())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .verifiedAt(verifiedAt)
                .streakDays(streakDays)
                .build();
    }

}
