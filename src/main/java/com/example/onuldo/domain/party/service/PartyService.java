package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Settlement;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.SettlementRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.challenge.support.ParticipationValidator;
import com.example.onuldo.domain.challenge.service.ChallengeNotificationService;
import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.dto.response.PartyMemberResultResDto;
import com.example.onuldo.domain.party.dto.response.PartyMemberVerificationResDto;
import com.example.onuldo.domain.party.dto.response.PartyResultResDto;
import com.example.onuldo.domain.party.dto.response.PartyWaitingResDto;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyChallengeId;
import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.entity.PartyMemberId;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartySettlementResultType;
import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.party.repository.PartyChallengeRepository;
import com.example.onuldo.domain.party.repository.PartyMemberRepository;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.exception.InsufficientPointException;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import com.example.onuldo.domain.challenge.enums.VerificationReviewStatus;
import com.example.onuldo.domain.party.dto.response.PartyHomeItemResDto;
import com.example.onuldo.domain.party.dto.response.PartyHomeMemberResDto;
import com.example.onuldo.domain.party.dto.response.PartyHomeResDto;
import com.example.onuldo.domain.party.dto.response.PartySettlementBannerResDto;
import com.example.onuldo.domain.party.enums.PartyHomeCardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyService {

    private static final int INVITE_CODE_LENGTH = 6;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_INVITE_CODE_RETRY = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // PAR-02: 모집 인원은 2~5명 (방장 포함)
    private static final int MIN_MEMBERS = 2;
    private static final int MAX_MEMBERS = 5;

    private static final int DAYS_PER_WEEK = 7;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final SettlementRepository settlementRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChallengeNotificationService challengeNotificationService;
    private final TimeService timeService;
    private final ParticipationValidator participationValidator;

    public PartyCreateResDto createParty(Long userId, PartyCreateReqDto request) {
        if (request.maxMembers() < MIN_MEMBERS || request.maxMembers() > MAX_MEMBERS) {
            throw new RestApiException(GlobalErrorStatus._INVALID_MAX_MEMBERS);
        }

        User host = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        Challenge challenge = challengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        participationValidator.validateNotOngoing(userId, challenge.getId());

        challenge.validateDurationOption(request.durationWeeks());
        challenge.validateDepositOption(request.depositAmount());

        if (host.getPointBalance() < request.depositAmount()) {
            throw new InsufficientPointException(
                    GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY,
                    host.getPointBalance(),
                    request.depositAmount()
            );
        }

        String inviteCode = generateUniqueInviteCode();
        int durationDays = request.durationWeeks() * DAYS_PER_WEEK;
        LocalDateTime inviteExpiresAt = timeService.nowKst().plusDays(durationDays);

        Party party = Party.builder()
                .name(request.name())
                .hostUser(host)
                .inviteCode(inviteCode)
                .inviteExpiresAt(inviteExpiresAt)
                .maxMembers(request.maxMembers())
                .durationDays(durationDays)
                .depositAmount(request.depositAmount())
                .build();
        partyRepository.save(party);

        // Party가 먼저 영속화되어 party.getId()가 채워진 뒤에 PartyMember/PartyChallenge 저장
        // (PartyMember, PartyChallenge 둘 다 @MapsId로 Party의 PK를 물고 있어서 순서 중요)
        PartyMember hostMember = PartyMember.builder()
                .id(new PartyMemberId(party.getId(), host.getId()))
                .party(party)
                .user(host)
                .role(PartyMemberRole.HOST)
                .build();
        partyMemberRepository.save(hostMember);

        PartyChallenge partyChallenge = PartyChallenge.builder()
                .id(new PartyChallengeId(party.getId(), challenge.getId()))
                .party(party)
                .challenge(challenge)
                .build();
        partyChallengeRepository.save(partyChallenge);

        return PartyCreateResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .inviteCode(party.getInviteCode())
                .inviteExpiresAt(party.getInviteExpiresAt())
                .status(party.getStatus())
                .hostUserId(party.getHostUser().getId())
                .maxMembers(party.getMaxMembers())
                .createdAt(party.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<PartyListResDto> getMyParties(Long userId, String cursor, int size) {
        int resolvedSize = CursorConstants.resolveSize(size);

        int offset = 0;
        if (!CursorKeyCodec.isBlank(cursor)) {
            offset = CursorKeyCodec.toIntCursorValue(CursorKeyCodec.decodeAsLongs(cursor, 1)[0]);
        }

        LocalDate today = timeService.todayKst();
        List<Object[]> rows = partyRepository.findMyPartiesExcludingWaiting(userId, today);

        List<Long> partyIds = rows.stream().map(row -> (Long) row[0]).toList();
        Map<Long, List<PartyMemberVerificationResDto>> membersByParty =
                loadMembersWithTodayVerification(partyIds, today);
        Map<Long, Verification> myVerificationByParty =
                loadMyTodayVerification(partyIds, userId, today);

        // HOME-09(상태 우선순위 → 마감 시각 → D-day → 챌린지ID)와 동일한 기준으로 정렬한다.
        // "오늘 나의 인증 상태"가 DB 컬럼이 아닌 계산값이라 결과 전체를 가져와 서비스에서 정렬하고,
        // 커서는 이 정렬된 목록 안에서의 위치(오프셋)를 가리킨다.
        List<PartyListSortEntry> sorted = rows.stream()
                .map(row -> toSortEntry(row, today, membersByParty, myVerificationByParty))
                .sorted(Comparator
                        .comparingInt((PartyListSortEntry entry) -> statusPriority(entry.item().myStatus()))
                        .thenComparing(
                                entry -> entry.item().verificationDeadline(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                entry -> entry.item().endDate(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(PartyListSortEntry::challengeId))
                .toList();

        int fromIndex = Math.min(offset, sorted.size());
        int toIndex = Math.min(fromIndex + resolvedSize + 1, sorted.size());
        List<PartyListSortEntry> page = sorted.subList(fromIndex, toIndex);

        int nextOffset = fromIndex + resolvedSize;
        return CursorPageResponse.of(
                page,
                resolvedSize,
                PartyListSortEntry::item,
                ignored -> CursorKeyCodec.encode(nextOffset)
        );
    }

    // 정렬 기준(챌린지ID)을 응답 DTO에 노출하지 않으면서 함께 들고 다니기 위한 내부 전용 래퍼
    private record PartyListSortEntry(PartyListResDto item, Long challengeId) {
    }

    private PartyListSortEntry toSortEntry(
            Object[] row,
            LocalDate today,
            Map<Long, List<PartyMemberVerificationResDto>> membersByParty,
            Map<Long, Verification> myVerificationByParty
    ) {
        Long partyId = (Long) row[0];
        Long challengeId = (Long) row[11];
        PartyListResDto item = toPartyListResDto(row, today, membersByParty, myVerificationByParty.get(partyId));
        return new PartyListSortEntry(item, challengeId);
    }

    private Map<Long, List<PartyMemberVerificationResDto>> loadMembersWithTodayVerification(
            List<Long> partyIds, LocalDate today
    ) {
        if (partyIds.isEmpty()) {
            return Map.of();
        }

        // 파티별로 오늘 PASS한 유저를 분리 집계 — 한 유저가 여러 파티에 동시 참여 중이면
        // 파티 구분 없는 단일 Set으로는 A파티 인증이 B파티 카드에도 "인증완료"로 새어 나간다.
        Map<Long, Set<Long>> verifiedUserIdsByParty = verificationRepository
                .findTodayAutoPassVerificationsByPartyIdIn(partyIds, today).stream()
                .collect(Collectors.groupingBy(
                        verification -> verification.getParticipation().getParty().getId(),
                        Collectors.mapping(
                                verification -> verification.getParticipation().getUser().getId(),
                                Collectors.toSet()
                        )
                ));

        return partyMemberRepository.findByParty_IdInOrderByJoinedAtAsc(partyIds).stream()
                .collect(Collectors.groupingBy(
                        member -> member.getParty().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                member -> PartyMemberVerificationResDto.builder()
                                        .userId(member.getUser().getId())
                                        .nickname(member.getUser().getNickname())
                                        .profileImageUrl(member.getUser().getProfileImageUrl())
                                        .isVerifiedToday(verifiedUserIdsByParty
                                                .getOrDefault(member.getParty().getId(), Set.of())
                                                .contains(member.getUser().getId()))
                                        .build(),
                                Collectors.toList()
                        )
                ));
    }

    // 같은 날 재검토로 파티당 여러 건이 나와도 먼저 조회된 것을 유지 (파티당 1건 기준)
    private Map<Long, Verification> loadMyTodayVerification(List<Long> partyIds, Long userId, LocalDate today) {
        if (partyIds.isEmpty()) {
            return Map.of();
        }

        return verificationRepository.findTodayVerificationsByPartyIdInAndUserId(partyIds, userId, today).stream()
                .collect(Collectors.toMap(
                        v -> v.getParticipation().getParty().getId(),
                        v -> v,
                        (existing, duplicate) -> existing
                ));
    }

    private PartyListResDto toPartyListResDto(
            Object[] row,
            LocalDate today,
            Map<Long, List<PartyMemberVerificationResDto>> membersByParty,
            Verification myVerification
    ) {
        Long partyId = (Long) row[0];
        PartyStatus status = (PartyStatus) row[4];
        LocalDate startDate = (LocalDate) row[5];
        LocalDate endDate = (LocalDate) row[6];
        LocalTime verificationDeadline = (LocalTime) row[7];
        int totalMemberCount = ((Long) row[8]).intValue();
        int verifiedMemberCount = ((Long) row[9]).intValue();
        long windowPassCount = (Long) row[10];

        return PartyListResDto.builder()
                .partyId(partyId)
                .name((String) row[1])
                .challengeTitle((String) row[2])
                .goal((String) row[3])
                .status(status)
                .myStatus(resolveMyStatus(myVerification, verificationDeadline, today))
                .endDate(endDate)
                .dDay(calculateDDay(endDate, today))
                .verificationDeadline(verificationDeadline)
                .progressRate(calculateTeamProgressRate(
                        status, startDate, endDate, totalMemberCount, windowPassCount, today))
                .verifiedMemberCount(verifiedMemberCount)
                .totalMemberCount(totalMemberCount)
                .members(membersByParty.getOrDefault(partyId, List.of()))
                .build();
    }

    // 오늘 나의 인증 상태 — 홈 "함께하는 파티" 카드 정책(generatePartyHomeItem)과 동일 기준
    private PartyHomeCardStatus resolveMyStatus(Verification myVerification, LocalTime deadline, LocalDate today) {
        if (myVerification == null) {
            LocalDateTime deadlineAt = deadline != null ? LocalDateTime.of(today, deadline) : null;
            boolean beforeDeadline = deadlineAt == null || timeService.nowKst().isBefore(deadlineAt);
            return beforeDeadline ? PartyHomeCardStatus.NOT_VERIFIED : PartyHomeCardStatus.FAIL;
        }
        return switch (myVerification.getReview()) {
            case PASS -> PartyHomeCardStatus.SUCCESS;
            case AUTO_FAIL -> PartyHomeCardStatus.FAIL;
            case PENDING, MANUAL_REVIEW -> PartyHomeCardStatus.PENDING;
        };
    }

    private int calculateDDay(LocalDate endDate, LocalDate today) {
        if (endDate == null) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(today, endDate));
    }

    // REC-02 팀 달성률: 팀 전체 PASS 인증 수 ÷ (인원 × 판정 기준일수).
    // 진행중은 분모가 경과일수, 완료는 전체 진행일수. (수행일은 시작일 다음 날부터 산정)
    private double calculateTeamProgressRate(
            PartyStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int totalMemberCount,
            long windowPassCount,
            LocalDate today
    ) {
        if (startDate == null || endDate == null || totalMemberCount <= 0) {
            return 0.0;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays <= 0) {
            return 0.0;
        }

        long denominatorDays = status == PartyStatus.FINISHED
                ? totalDays
                : elapsedPerformanceDays(startDate, endDate, today);
        if (denominatorDays <= 0) {
            return 0.0;
        }

        double rate = (double) windowPassCount / (totalMemberCount * denominatorDays);
        return Math.round(rate * 100.0) / 100.0;
    }

    private long elapsedPerformanceDays(LocalDate startDate, LocalDate endDate, LocalDate today) {
        LocalDate cappedToday = today.isAfter(endDate) ? endDate : today;
        long elapsed = ChronoUnit.DAYS.between(startDate, cappedToday);
        return Math.max(elapsed, 0);
    }

    @Transactional(readOnly = true)
    public PartyWaitingResDto getPartyWaiting(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        if (party.getStatus() == PartyStatus.DISSOLVED) {
            throw new RestApiException(GlobalErrorStatus._PARTY_DISSOLVED);
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        boolean isRequesterMember = partyMembers.stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isRequesterMember) {
            throw new RestApiException(GlobalErrorStatus._NOT_PARTY_MEMBER);
        }

        return PartyWaitingResDto.of(party, partyMembers, userId);
    }

    // PAR-04, PAR-ERR-01: 초대코드 검증 후 파티 참여
    public PartyWaitingResDto joinParty(Long userId, PartyJoinReqDto request) {
        Party party = partyRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._INVALID_INVITE_CODE));

        if (party.getStatus() != PartyStatus.WAITING) {
            throw new RestApiException(GlobalErrorStatus._PARTY_ALREADY_STARTED);
        }

        int currentMembers = partyMemberRepository.countByParty_Id(party.getId());
        if (currentMembers >= party.getMaxMembers()) {
            throw new RestApiException(GlobalErrorStatus._PARTY_FULL);
        }

        if (party.getInviteExpiresAt() != null && party.getInviteExpiresAt().isBefore(timeService.nowKst())) {
            throw new RestApiException(GlobalErrorStatus._INVITE_CODE_EXPIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        // 파티 중복 참여 방지용 코드 (정책서 근거 없음)
        if (partyMemberRepository.existsByParty_IdAndUser_Id(party.getId(), userId)) {
            throw new RestApiException(GlobalErrorStatus._ALREADY_PARTY_MEMBER);
        }

        PartyMember member = PartyMember.builder()
                .id(new PartyMemberId(party.getId(), userId))
                .party(party)
                .user(user)
                .role(PartyMemberRole.MEMBER)
                .build();
        partyMemberRepository.save(member);

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(party.getId());
        return PartyWaitingResDto.of(party, partyMembers, userId);
    }

    // 준비완료 전환 API는 파티 API 목록(7개)에 명시되어 있지 않았으나 PAR-05, PAR-ERR-03 근거로 추가함 (BE 확인 필요)
    // PAR-05, PAR-ERR-03: 파티원 준비완료/대기 상태 토글
    public PartyWaitingResDto togglePartyMemberReady(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository.findById(new PartyMemberId(partyId, userId))
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._NOT_PARTY_MEMBER));

        // PAR-05: 방장은 준비완료 대상 아님
        if (member.getRole() == PartyMemberRole.HOST) {
            throw new RestApiException(GlobalErrorStatus._HOST_CANNOT_READY);
        }

        if (member.isReady()) {
            member.waiting();
        } else {
            // PAR-ERR-03: 준비완료 클릭 시점에 보유 포인트 < 도전금이면 전환 불가
            User user = member.getUser();
            if (user.getPointBalance() < party.getDepositAmount()) {
                throw new RestApiException(GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY);
            }
            member.ready();
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);
        return PartyWaitingResDto.of(party, partyMembers, userId);
    }

    // PAR-05: 파티 시작 (방장만 가능, 2인 이상 + 전원 준비완료 시 활성화, 전원 도전금 일괄 예치)
    public PartyStartResDto startParty(Long partyId, Long userId) {
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        if (!party.getHostUser().getId().equals(userId)) {
            throw new RestApiException(GlobalErrorStatus._NOT_PARTY_HOST);
        }

        if (party.getStatus() != PartyStatus.WAITING) {
            throw new RestApiException(GlobalErrorStatus._PARTY_ALREADY_STARTED);
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        // PAR-05: 모집 최대 인원 도달 여부와 무관하게, 2인 이상 + 현재 참여 파티원 전원 준비완료 시 시작 가능
        boolean allMembersReady = partyMembers.stream()
                .filter(member -> member.getRole() == PartyMemberRole.MEMBER)
                .allMatch(PartyMember::isReady);
        if (partyMembers.size() < MIN_MEMBERS_TO_START || !allMembersReady) {
            throw new RestApiException(GlobalErrorStatus._PARTY_NOT_READY_TO_START);
        }

        String challengeName = partyChallengeRepository.findByParty_Id(partyId)
                .map(partyChallenge -> partyChallenge.getChallenge().getName())
                .orElse(party.getName());

        // PAR-05: 시작 시 파티원 전원 도전금 일괄 예치
        // (부족한 파티원이 있으면 예외 발생 → @Transactional에 의해 그 전에 차감된 파티원분까지 전부 롤백됨)
        List<Long> memberUserIds = partyMembers.stream()
                .map(member -> member.getUser().getId())
                .sorted()
                .toList();

        for (Long memberUserId : memberUserIds) {
            User user = userRepository.findByIdForUpdate(memberUserId)
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

            if (user.getPointBalance() < party.getDepositAmount()) {
                throw new RestApiException(GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY);
            }

            long balanceAfter = user.getPointBalance() - party.getDepositAmount();
            user.setPointBalance(balanceAfter);
            userRepository.save(user);

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(user)
                    .type(PointTransactionType.DEPOSIT)
                    .amount(-party.getDepositAmount())
                    .balanceAfter(balanceAfter)
                    .description(challengeName)
                    .build()
            );
        }

        // PAR-05: 시작 시 상태 전환 + 초대코드 만료 (시작 후 초대코드 만료·파티원 추가 불가)
        LocalDateTime now = timeService.nowKst();
        party.updateStatus(PartyStatus.ONGOING);
        party.updateStartTriggeredAt(now);
        party.updateInviteExpiresAt(now);
        partyRepository.save(party);

        // PAR-05: 시작 시 챌린지 진행 시작 — 파티원별 Participation(참여 기록) 생성
        // (인증/진행 피드 조회가 이 Participation을 기준으로 동작하므로 시작 시점에 반드시 생성되어야 함)
        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));
        Challenge challenge = partyChallenge.getChallenge();

        LocalDate startDate = timeService.todayKst();
        LocalDate endDate = startDate.plusDays(party.getDurationDays());
        int durationWeeks = party.getDurationDays() / DAYS_PER_WEEK;

        for (PartyMember member : partyMembers) {
            Participation participation = Participation.builder()
                    .user(member.getUser())
                    .challenge(challenge)
                    .party(party)
                    .participationType(ParticipationType.PARTY)
                    .depositAmount(party.getDepositAmount())
                    .durationWeeks(durationWeeks)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            validateParticipationState(participation);
            participationRepository.save(participation);
            challengeNotificationService.scheduleChallengeLifecycleNotifications(participation);
        }

        return PartyStartResDto.of(party);
    }

    private void validateParticipationState(Participation participation) {
        if (participation.getParticipationType() == ParticipationType.PERSONAL && participation.getParty() != null) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "개인 참여에는 party가 연결되면 안 됩니다.");
        }

        if (participation.getParticipationType() == ParticipationType.PARTY && participation.getParty() == null) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "party 참여에는 party가 필요합니다.");
        }
    }

    // 파티 진행 피드: 팀 진행률(오늘 인증 완료 비율)과 파티원별 오늘 인증 현황 조회
    @Transactional(readOnly = true)
    public PartyFeedResDto getPartyFeed(Long partyId, Long userId) {
    // 정산 결과 조회 시 홈 배너 확인 처리(Settlement.confirm())가 함께 일어나므로 readOnly 불가
    @Transactional
    public PartyResultResDto getPartyResult(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        boolean isRequesterMember = partyMembers.stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isRequesterMember) {
            throw new RestApiException(GlobalErrorStatus._NOT_PARTY_MEMBER);
        }

        List<Settlement> settlements = settlementRepository.findByPartyId(partyId);
        // 동일 유저의 정산 데이터가 중복 저장된 경우를 대비해 첫 값만 유지
        Map<Long, Settlement> settlementByUserId = settlements.stream()
                .collect(Collectors.toMap(
                        s -> s.getParticipation().getUser().getId(),
                        s -> s,
                        (existing, duplicate) -> existing
                ));

        // ONGOING/CANCELED 상태가 섞여 있으면 아직 정산이 확정되지 않은 것으로 간주
        List<PartyMemberResultResDto> members = new ArrayList<>();
        for (PartyMember member : partyMembers) {
            Settlement settlement = settlementByUserId.get(member.getUser().getId());
            if (settlement == null) {
                throw new RestApiException(GlobalErrorStatus._SETTLEMENT_NOT_COMPLETED);
            }

            ParticipationStatus status = settlement.getParticipation().getStatus();
            if (status != ParticipationStatus.SUCCESS && status != ParticipationStatus.FAIL) {
                throw new RestApiException(GlobalErrorStatus._SETTLEMENT_NOT_COMPLETED);
            }

            members.add(generatePartyMemberResult(member.getUser(), status, settlement));
        }

        boolean allSuccess = members.stream()
                .allMatch(m -> m.status() == ParticipationStatus.SUCCESS);
        boolean allFail = members.stream()
                .allMatch(m -> m.status() == ParticipationStatus.FAIL);
        PartySettlementResultType resultType = allSuccess
                ? PartySettlementResultType.ALL_SUCCESS
                : allFail
                  ? PartySettlementResultType.ALL_FAIL
                  : PartySettlementResultType.PARTIAL_SUCCESS;

        Settlement mySettlement = settlementByUserId.get(userId);
        mySettlement.confirm();
        PartyMemberResultResDto myResult = members.stream()
                .filter(m -> m.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._NOT_PARTY_MEMBER));

        return PartyResultResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .resultType(resultType)
                .myDepositAmount(mySettlement.getDepositAmount())
                .myDisplayAmount(myResult.displayAmount())
                .members(members)
                .build();
    }

    private String generateUniqueInviteCode() {
        String code;
        int attempts = 0;
        do {
            code = generateRandomCode();
            attempts++;
            if (attempts > MAX_INVITE_CODE_RETRY) {
                throw new RestApiException(GlobalErrorStatus._INVITE_CODE_GENERATION_FAILED);
            }
        } while (partyRepository.existsByInviteCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    // 성공: 성과 보너스 + 재분배 몫 / 실패: 환급액 - 도전금(손실)
    private PartyMemberResultResDto generatePartyMemberResult(User user, ParticipationStatus status, Settlement settlement) {
        int displayAmount = status == ParticipationStatus.SUCCESS
                ? settlement.getBonusAmount() + settlement.getPartyShareAmount()
                : settlement.getRefundAmount() - settlement.getDepositAmount();

        return PartyMemberResultResDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .status(status)
                .displayAmount(displayAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public PartyHomeResDto getHomeParties(Long userId) {
        List<Party> ongoingParties = partyRepository.findOngoingPartiesByUserId(userId);

        List<HomeItemWithChallengeId> wrappedItems = ongoingParties.stream()
                .map(party -> generatePartyHomeItem(party, userId))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        // HOME-09: 상태 우선순위(미인증→검토대기→성공→실패) → 마감 빠른순 → D-day 짧은순 → 챌린지ID 오름차순
        wrappedItems.sort(java.util.Comparator
                .comparingInt((HomeItemWithChallengeId w) -> statusPriority(w.item().status()))
                .thenComparing(
                        (HomeItemWithChallengeId w) -> w.item().verificationDeadline(),
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                )
                .thenComparing(
                        (HomeItemWithChallengeId w) -> w.item().endDate(),
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
                )
                .thenComparing(HomeItemWithChallengeId::challengeId));

        List<PartyHomeItemResDto> parties = wrappedItems.stream()
                .map(HomeItemWithChallengeId::item)
                .toList();

        return PartyHomeResDto.builder()
                .settlementBanners(resolveSettlementBanners(userId))
                .parties(parties)
                .build();
    }

    // 아직 확인하지 않은 정산 전부 노출 — 결과 조회 시 confirm 처리되어 다음 응답부터 사라짐
    private List<PartySettlementBannerResDto> resolveSettlementBanners(Long userId) {
        return settlementRepository.findUnconfirmedPartySettlementsByUserId(userId)
                .stream()
                .map(settlement -> {
                    Party party = settlement.getParticipation().getParty();
                    return PartySettlementBannerResDto.builder()
                            .partyId(party.getId())
                            .partyName(party.getName())
                            .build();
                })
                .toList();
    }

    // 정렬 기준(챌린지ID)을 응답 DTO에 노출하지 않으면서 함께 들고 다니기 위한 내부 전용 래퍼
    private record HomeItemWithChallengeId(PartyHomeItemResDto item, Long challengeId) {
    }

    private int statusPriority(PartyHomeCardStatus status) {
        return switch (status) {
            case NOT_VERIFIED -> 0;
            case PENDING -> 1;
            case SUCCESS -> 2;
            case FAIL -> 3;
        };
    }

    private HomeItemWithChallengeId generatePartyHomeItem(Party party, Long userId) {
        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(party.getId())
                .orElse(null);
        if (partyChallenge == null) {
            return null;
        }
        Challenge challenge = partyChallenge.getChallenge();

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(party.getId());

        LocalDate today = timeService.todayKst();
        List<Verification> todayVerifications =
                verificationRepository.findTodayVerificationsByPartyId(party.getId(), today);

        // 파티원 1인당 하루 1회 인증이 원칙이므로 userId 기준으로 매핑
        Map<Long, Verification> verificationByUserId = todayVerifications.stream()
                .collect(Collectors.toMap(
                        v -> v.getParticipation().getUser().getId(),
                        v -> v,
                        (existing, duplicate) -> existing
                ));

        List<PartyHomeMemberResDto> members = partyMembers.stream()
                .map(member -> {
                    Verification verification = verificationByUserId.get(member.getUser().getId());
                    // HOME-07: 파티원 아바타의 "인증 완료"는 #15 피드와 동일하게 PASS 기준
                    boolean isVerified = verification != null
                            && verification.getReview() == VerificationReviewStatus.PASS;
                    return PartyHomeMemberResDto.builder()
                            .userId(member.getUser().getId())
                            .profileImageUrl(member.getUser().getProfileImageUrl())
                            .isVerifiedToday(isVerified)
                            .build();
                })
                .toList();

        // 서버 시간대가 KST가 아닐 수 있으므로 now/today 모두 TimeService 기준으로 통일
        LocalDateTime now = timeService.nowKst();
        LocalTime deadline = challenge.getTimeEnd();
        // 자정 경계를 포함해 마감 임박 여부를 판단하기 위해 날짜가 결합된 deadlineAt을 사용
        // (LocalTime만으로 minusHours(1)을 계산하면 마감이 자정 부근일 때 전날 시각으로 순환해버림)
        LocalDateTime deadlineAt = deadline != null ? LocalDateTime.of(today, deadline) : null;
        // HOME-03: 남은 시간 칩은 마감 1시간 전부터 표시
        boolean showRemainingTime = deadlineAt != null
                && !now.isBefore(deadlineAt.minusHours(1))
                && now.isBefore(deadlineAt);

        Verification myVerification = verificationByUserId.get(userId);
        PartyHomeCardStatus status;
        LocalDateTime verifiedAt = null;

        if (myVerification == null) {
            // HOME-04: 오늘 인증 미완료 + 마감 시각 전이면 [인증하기] 노출
            // 마감 후 미인증은 오늘 하루치 실패로 표시함 (챌린지 전체 SUCCESS/FAIL과는 무관, 화면 표시용 판단)
            boolean beforeDeadline = deadlineAt == null || now.isBefore(deadlineAt);
            status = beforeDeadline ? PartyHomeCardStatus.NOT_VERIFIED : PartyHomeCardStatus.FAIL;
        } else {
            status = switch (myVerification.getReview()) {
                case PASS -> PartyHomeCardStatus.SUCCESS;
                case AUTO_FAIL -> PartyHomeCardStatus.FAIL;
                case PENDING, MANUAL_REVIEW -> PartyHomeCardStatus.PENDING;
            };
            if (status == PartyHomeCardStatus.SUCCESS) {
                verifiedAt = myVerification.getVerifiedAt();
            }
        }

        // endDate는 startParty()에서 생성된 Participation.endDate를 단일 원본으로 사용한다
        // (party.getStartTriggeredAt() 기준으로 재계산하면 익일(+1일) 반영이 빠져 종료일이 하루 어긋남)
        LocalDate endDate = participationRepository
                .findByParty_IdAndUser_IdAndParticipationType(party.getId(), userId, ParticipationType.PARTY)
                .map(Participation::getEndDate)
                .orElse(null);

        PartyHomeItemResDto item = PartyHomeItemResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .challengeTitle(challenge.getName())
                .endDate(endDate)
                .verificationDeadline(deadline)
                .showRemainingTime(showRemainingTime)
                .status(status)
                .verifiedAt(verifiedAt)
                .members(members)
                .build();

        return new HomeItemWithChallengeId(item, challenge.getId());
    }

}