package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Challenge;
import com.example.onuldo.domain.challenge.entity.Participation;
import com.example.onuldo.domain.challenge.enums.ParticipationType;
import com.example.onuldo.domain.challenge.repository.ParticipationRepository;
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
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
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

    public PartyStartResDto startParty(Long partyId, Long userId) {
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._PARTY_NOT_FOUND));

        if (!party.getHostUser().getId().equals(userId)) {
            throw new RestApiException(GlobalErrorStatus._NOT_PARTY_HOST);
        }

        if (party.getStatus() != PartyStatus.WAITING) {
            throw switch (party.getStatus()) {
                case ONGOING -> new RestApiException(GlobalErrorStatus._PARTY_ALREADY_STARTED);
                case FINISHED -> new RestApiException(GlobalErrorStatus._PARTY_ALREADY_FINISHED);
                case DISSOLVED -> new RestApiException(GlobalErrorStatus._PARTY_DISSOLVED);
                default -> new RestApiException(GlobalErrorStatus._BAD_REQUEST);
            };
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);

        // 모집 최대 인원 도달 여부와 무관하게, 2인 이상 + 현재 참여 파티원 전원 준비완료 시 시작 가능
        boolean allMembersReady = partyMembers.stream()
                .filter(member -> member.getRole() == PartyMemberRole.MEMBER)
                .allMatch(PartyMember::isReady);
        if (partyMembers.size() < MIN_MEMBERS_TO_START || !allMembersReady) {
            throw new RestApiException(GlobalErrorStatus._PARTY_NOT_READY_TO_START);
        }

        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(partyId)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._CHALLENGE_NOT_FOUND));
        Challenge challenge = partyChallenge.getChallenge();
        String challengeName = challenge.getName();

        // 부족한 파티원이 있으면 예외 발생 → @Transactional에 의해 그 전에 차감된 파티원분까지 전부 롤백됨
        List<Long> memberUserIds = partyMembers.stream()
                .map(member -> member.getUser().getId())
                .sorted()
                .toList();

        for (Long memberUserId : memberUserIds) {
            User user = userRepository.findByIdForUpdate(memberUserId)
                    .orElseThrow(() -> new RestApiException(GlobalErrorStatus._USER_NOT_FOUND));

            if (user.getPointBalance() < party.getDepositAmount()) {
                throw new InsufficientPointException(
                        GlobalErrorStatus._INSUFFICIENT_POINT_FOR_PARTY,
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
        }

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
}
