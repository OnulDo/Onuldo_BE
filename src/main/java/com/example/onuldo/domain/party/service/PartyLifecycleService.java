package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
import com.example.onuldo.domain.challenge.service.ChallengeNotificationService;
import com.example.onuldo.domain.challenge.support.ParticipationValidator;
import com.example.onuldo.domain.party.dto.response.PartyStartResDto;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyMember;
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
import com.example.onuldo.global.common.exception.InsufficientPointException;
import com.example.onuldo.global.common.exception.BusinessRuleException;
import com.example.onuldo.global.common.exception.DuplicateException;
import com.example.onuldo.global.common.exception.ForbiddenException;
import com.example.onuldo.global.common.exception.InvalidRequestException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyLifecycleService {

    private static final int MIN_MEMBERS_TO_START = 2;

    // 파티 진행 기간은 2/4/8/12주 단위(일수 14/28/56/84)로만 생성되어 7로 나누어떨어짐
    private static final int DAYS_PER_WEEK = 7;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TimeService timeService;
    private final ParticipationValidator participationValidator;
    private final ChallengeNotificationService challengeNotificationService;

    public PartyStartResDto startParty(Long partyId, Long userId) {
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTY_NOT_FOUND));

        if (!party.getHostUser().getId().equals(userId)) {
            throw new ForbiddenException(ErrorStatus._NOT_PARTY_HOST);
        }

        if (party.getStatus() != PartyStatus.WAITING) {
            throw switch (party.getStatus()) {
                case ONGOING -> new DuplicateException(ErrorStatus._PARTY_ALREADY_STARTED);
                case FINISHED -> new DuplicateException(ErrorStatus._PARTY_ALREADY_FINISHED);
                case DISSOLVED -> new BusinessRuleException(ErrorStatus._PARTY_DISSOLVED);
                default -> new InvalidRequestException(ErrorStatus._BAD_REQUEST);
            };
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        // 모집 최대 인원 도달 여부와 무관하게, 2인 이상 + 현재 참여 파티원 전원 준비완료 시 시작 가능
        boolean allMembersReady = partyMembers.stream()
                .filter(member -> member.getRole() == PartyMemberRole.MEMBER)
                .allMatch(PartyMember::isReady);
        if (partyMembers.size() < MIN_MEMBERS_TO_START || !allMembersReady) {
            throw new BusinessRuleException(ErrorStatus._PARTY_NOT_READY_TO_START);
        }

        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(partyId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));
        Challenge challenge = partyChallenge.getChallenge();
        String challengeName = challenge.getName();

        LocalDateTime now = timeService.nowKst();
        party.updateStatus(PartyStatus.ONGOING);
        party.updateStartTriggeredAt(now);
        party.updateInviteExpiresAt(now);
        partyRepository.save(party);

        // #94: 참여 시작일은 시작 처리 다음 날부터 (당일 시작 금지, 시작 전 인증 방지)
        // durationDays는 시작일·종료일을 포함한 총 수행일수 (ParticipationRecordService.calculateInclusiveDays와 동일 기준)
        LocalDate startDate = now.toLocalDate().plusDays(1);
        LocalDate endDate = startDate.plusDays(party.getDurationDays() - 1);
        int durationWeeks = party.getDurationDays() / DAYS_PER_WEEK;

        // 부족한 파티원이 있거나 이미 다른 곳에서 해당 챌린지를 진행 중인 파티원이 있으면 예외 발생
        // → @Transactional에 의해 그 전에 차감·생성된 파티원분(및 위 party 상태 변경)까지 전부 롤백됨
        List<Long> memberUserIds = partyMembers.stream()
                .map(member -> member.getUser().getId())
                .sorted()
                .toList();

        for (Long memberUserId : memberUserIds) {
            createPartyParticipation(memberUserId, party, challenge, challengeName, durationWeeks, startDate, endDate);
        }

        return PartyStartResDto.of(party);
    }

    // participatePersonalChallenge와 동일하게 사용자 행을 먼저 잠근 뒤 검증·차감·저장을
    // 한 트랜잭션 안에서 원자적으로 수행해, 파티 시작과 동시에 이뤄지는 다른 참여 시도와의
    // 경합에서도 중복 Participation이 생기지 않도록 한다.
    // 호출부(startParty)에서 정렬된 memberUserIds 순서로 호출해야 락 획득 순서(데드락 방지)가 유지된다.
    private void createPartyParticipation(
            Long memberUserId,
            Party party,
            Challenge challenge,
            String challengeName,
            int durationWeeks,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User user = userRepository.findByIdForUpdate(memberUserId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        participationValidator.validateNotOngoing(memberUserId, challenge.getId());

        if (user.getPointBalance() < party.getDepositAmount()) {
            throw new InsufficientPointException(
                    ErrorStatus._INSUFFICIENT_POINT_FOR_PARTY,
                    user.getPointBalance(),
                    party.getDepositAmount()
            );
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

        Participation participation = Participation.builder()
                .user(user)
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

    private void validateParticipationState(Participation participation) {
        if (participation.getParticipationType() == ParticipationType.PERSONAL && participation.getParty() != null) {
            throw new InvalidRequestException(ErrorStatus._PARTICIPATION_PARTY_NOT_ALLOWED);
        }

        if (participation.getParticipationType() == ParticipationType.PARTY && participation.getParty() == null) {
            throw new InvalidRequestException(ErrorStatus._PARTICIPATION_PARTY_REQUIRED);
        }
    }
}
