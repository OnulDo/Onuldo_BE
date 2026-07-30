package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.request.PartyJoinReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyFeedItemResDto;
import com.example.onuldo.domain.party.dto.response.PartyFeedResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.dto.response.PartyStartResDto;
import com.example.onuldo.domain.party.dto.response.PartyWaitingResDto;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyChallengeId;
import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.entity.PartyMemberId;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.party.repository.PartyChallengeRepository;
import com.example.onuldo.domain.party.repository.PartyMemberRepository;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.PointTransaction;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.enums.PointTransactionType;
import com.example.onuldo.domain.user.repository.PointTransactionRepository;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyService {

    private static final int INVITE_CODE_LENGTH = 6;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_INVITE_CODE_RETRY = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // PAR-03: 파티 이름은 2~20자 한글·영문·숫자·공백만 허용
    private static final Pattern PARTY_NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9\\s]{2,20}$");

    // PAR-02: 모집 인원은 2~5명 (방장 포함)
    private static final int MIN_MEMBERS = 2;
    private static final int MAX_MEMBERS = 5;

    // PAR-05: [시작하기] 활성화를 위한 최소 인원 (방장 포함 2인 이상)
    private static final int MIN_MEMBERS_TO_START = 2;

    // 파티 진행 기간은 2/4/8/12주 단위(일수 14/28/56/84)로만 생성되어 7로 나누어떨어짐
    private static final int DAYS_PER_WEEK = 7;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ParticipationRepository participationRepository;
    private final VerificationRepository verificationRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public PartyCreateResDto createParty(Long userId, PartyCreateReqDto request) {
        // PAR-03: 파티 이름 규칙 검증 (2~20자 한글·영문·숫자·공백)
        if (!PARTY_NAME_PATTERN.matcher(request.name()).matches()) {
            throw new RestApiException(GlobalErrorStatus._INVALID_PARTY_NAME);
        }

        // PAR-02: 모집 인원 범위 검증 (2~5명)
        if (request.maxMembers() < MIN_MEMBERS || request.maxMembers() > MAX_MEMBERS) {
            throw new RestApiException(GlobalErrorStatus._INVALID_MAX_MEMBERS);
        }

        User host = userRepository.findById(userId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

        Challenge challenge = challengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        // PAR-ERR-02: 파티 생성 시 방장 보유 포인트 < 도전금이면 포인트 충전 안내
        if (host.getPointBalance() < request.depositAmount()) {
            throw new RestApiException(GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY);
        }

        String inviteCode = generateUniqueInviteCode();
        LocalDateTime inviteExpiresAt = LocalDateTime.now().plusDays(request.durationDays());

        Party party = Party.builder()
                .name(request.name())
                .hostUser(host)
                .inviteCode(inviteCode)
                .inviteExpiresAt(inviteExpiresAt)
                .maxMembers(request.maxMembers())
                .durationDays(request.durationDays())
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

        LocalDateTime lastCreatedAt = null;
        Long lastId = null;
        if (cursor != null) {
            String[] parts = CursorKeyCodec.decode(cursor);
            lastCreatedAt = new Timestamp(Long.parseLong(parts[0])).toLocalDateTime();
            lastId = Long.parseLong(parts[1]);
        }

        List<Object[]> rows = partyRepository.findMyPartiesExcludingWaiting(
                userId, lastCreatedAt, lastId, CursorPageable.of(resolvedSize)
        );

        return CursorPageResponse.of(
                rows,
                resolvedSize,
                row -> (PartyListResDto) row[0],
                row -> CursorKeyCodec.encode(
                        Timestamp.valueOf((LocalDateTime) row[1]).getTime(),
                        ((PartyListResDto) row[0]).partyId()
                )
        );
    }

    @Transactional(readOnly = true)
    public PartyWaitingResDto getPartyWaiting(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

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

        if (party.getInviteExpiresAt() != null && party.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
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
        Party party = partyRepository.findById(partyId)
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
        LocalDateTime now = LocalDateTime.now();
        party.updateStatus(PartyStatus.ONGOING);
        party.updateStartTriggeredAt(now);
        party.updateInviteExpiresAt(now);
        partyRepository.save(party);

        // PAR-05: 시작 시 챌린지 진행 시작 — 파티원별 Participation(참여 기록) 생성
        // (인증/진행 피드 조회가 이 Participation을 기준으로 동작하므로 시작 시점에 반드시 생성되어야 함)
        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));
        Challenge challenge = partyChallenge.getChallenge();

        LocalDate startDate = LocalDate.now();
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
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        boolean isRequesterMember = partyMembers.stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isRequesterMember) {
            throw new RestApiException(GlobalErrorStatus._NOT_PARTY_MEMBER);
        }

        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));

        List<Verification> todayVerifications =
                verificationRepository.findTodayAutoPassVerificationsByPartyId(partyId, LocalDate.now());

        // 파티원 1인당 하루 1회 인증이 원칙이므로 userId 기준으로 매핑
        Map<Long, Verification> verificationByUserId = todayVerifications.stream()
                .collect(Collectors.toMap(
                        v -> v.getParticipation().getUser().getId(),
                        v -> v
                ));

        List<PartyFeedItemResDto> members = partyMembers.stream()
                .map(member -> {
                    Verification verification = verificationByUserId.get(member.getUser().getId());
                    return verification != null
                            ? generateVerifiedFeedItem(member.getUser(), verification)
                            : generateNotVerifiedFeedItem(member.getUser());
                })
                .toList();

        int verifiedMemberCount = verificationByUserId.size();
        int totalMemberCount = partyMembers.size();
        double progressRate = totalMemberCount == 0 ? 0.0 : (double) verifiedMemberCount / totalMemberCount;

        return PartyFeedResDto.builder()
                .partyId(party.getId())
                .name(party.getName())
                .challengeTitle(partyChallenge.getChallenge().getName())
                .progressRate(progressRate)
                .verifiedMemberCount(verifiedMemberCount)
                .totalMemberCount(totalMemberCount)
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

    private PartyFeedItemResDto generateVerifiedFeedItem(User user, Verification verification) {
        return PartyFeedItemResDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .isVerifiedToday(true)
                .verificationPhotoUrl(verification.getPhotoUrl())
                .verifiedAt(verification.getVerifiedAt())
                .build();
    }

    private PartyFeedItemResDto generateNotVerifiedFeedItem(User user) {
        return PartyFeedItemResDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .isVerifiedToday(false)
                .verificationPhotoUrl(null)
                .verifiedAt(null)
                .build();
    }
}
