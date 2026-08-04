package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.repository.ChallengeRepository;
import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
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
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.domain.user.repository.UserRepository;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorKeyCodec;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.cursor.CursorPageable;
import com.example.onuldo.global.common.exception.InsufficientPointException;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

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

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final TimeService timeService;

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
            throw new InsufficientPointException(
                    GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY,
                    host.getPointBalance(),
                    request.depositAmount()
            );
        }

        String inviteCode = generateUniqueInviteCode();
        LocalDateTime inviteExpiresAt = timeService.nowKst().plusDays(request.durationDays());

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
        if (!CursorKeyCodec.isBlank(cursor)) {
            String[] parts = CursorKeyCodec.decodeParts(cursor, 2);
            try {
                lastCreatedAt = LocalDateTime.parse(parts[0]);
                lastId = Long.parseLong(parts[1]);
            } catch (DateTimeParseException | NumberFormatException e) {
                throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
            }
        }

        List<Object[]> rows = partyRepository.findMyPartiesExcludingWaiting(
                userId, lastCreatedAt, lastId, CursorPageable.of(resolvedSize)
        );

        return CursorPageResponse.of(
                rows,
                resolvedSize,
                row -> (PartyListResDto) row[0],
                row -> CursorKeyCodec.encode(
                        ((LocalDateTime) row[1]).toString(),
                        ((PartyListResDto) row[0]).partyId()
                )
        );
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
}
