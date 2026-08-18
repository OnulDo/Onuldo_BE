package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.support.ParticipationValidator;
import com.example.onuldo.domain.party.dto.request.PartyJoinReqDto;
import com.example.onuldo.domain.party.dto.request.PartyReadyReqDto;
import com.example.onuldo.domain.party.dto.response.PartyLeaveResDto;
import com.example.onuldo.domain.party.dto.response.PartyWaitingResDto;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.entity.PartyMemberId;
import com.example.onuldo.domain.party.enums.PartyMemberRole;
import com.example.onuldo.domain.party.enums.PartyStatus;
import com.example.onuldo.domain.party.repository.PartyChallengeRepository;
import com.example.onuldo.domain.party.repository.PartyMemberRepository;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.User;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyMemberService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final UserRepository userRepository;
    private final TimeService timeService;
    private final ParticipationValidator participationValidator;

    // 동시성 방어: 초대코드 조회 시점에 바로 비관적 락을 걸어, 상태 확인부터 저장까지를 하나의 조회로 직렬화한다.
    // (락 없이 조회 → 락 걸고 재조회하는 2단계 방식은 그 사이에 다른 트랜잭션이 파티를 시작시켜도
    //  먼저 읽은 상태값으로 통과해버리는 레이스 컨디션이 있어 단일 조회 방식으로 변경)
    public PartyWaitingResDto joinParty(Long userId, PartyJoinReqDto request) {
        Party party = partyRepository.findByInviteCodeForUpdate(request.inviteCode())
                .orElseThrow(() -> new InvalidRequestException(ErrorStatus._INVALID_INVITE_CODE));

        if (party.getStatus() != PartyStatus.WAITING) {
            throw switch (party.getStatus()) {
                case ONGOING -> new DuplicateException(ErrorStatus._PARTY_ALREADY_STARTED);
                case FINISHED -> new DuplicateException(ErrorStatus._PARTY_ALREADY_FINISHED);
                case DISSOLVED -> new BusinessRuleException(ErrorStatus._PARTY_DISSOLVED);
                default -> new InvalidRequestException(ErrorStatus._BAD_REQUEST);
            };
        }

        if (party.getInviteExpiresAt() != null && party.getInviteExpiresAt().isBefore(timeService.nowKst())) {
            throw new BusinessRuleException(ErrorStatus._INVITE_CODE_EXPIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._USER_NOT_FOUND));

        if (partyMemberRepository.existsByParty_IdAndUser_Id(party.getId(), userId)) {
            throw new DuplicateException(ErrorStatus._ALREADY_PARTY_MEMBER);
        }

        PartyChallenge partyChallenge = partyChallengeRepository.findByParty_Id(party.getId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus._CHALLENGE_NOT_FOUND));
        participationValidator.validateNotOngoing(userId, partyChallenge.getChallenge().getId());

        int currentMembers = partyMemberRepository.countByParty_Id(party.getId());
        if (currentMembers >= party.getMaxMembers()) {
            throw new BusinessRuleException(ErrorStatus._PARTY_FULL);
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

    public PartyLeaveResDto leaveParty(Long partyId, Long userId) {
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTY_NOT_FOUND));

        if (party.getStatus() != PartyStatus.WAITING) {
            throw switch (party.getStatus()) {
                case ONGOING -> new DuplicateException(ErrorStatus._PARTY_ALREADY_STARTED);
                case FINISHED -> new DuplicateException(ErrorStatus._PARTY_ALREADY_FINISHED);
                case DISSOLVED -> new BusinessRuleException(ErrorStatus._PARTY_DISSOLVED);
                default -> new InvalidRequestException(ErrorStatus._BAD_REQUEST);
            };
        }

        PartyMember leavingMember = partyMemberRepository.findById(new PartyMemberId(partyId, userId))
                .orElseThrow(() -> new ForbiddenException(ErrorStatus._NOT_PARTY_MEMBER));

        boolean wasHost = leavingMember.getRole() == PartyMemberRole.HOST;
        partyMemberRepository.delete(leavingMember);
        partyMemberRepository.flush();

        if (!wasHost) {
            return PartyLeaveResDto.builder()
                    .partyId(party.getId())
                    .dissolved(false)
                    .newHostUserId(null)
                    .build();
        }

        List<PartyMember> remainingMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId).stream()
                .filter(member -> !member.getUser().getId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
        if (remainingMembers.isEmpty()) {
            party.updateStatus(PartyStatus.DISSOLVED);
            party.updateInviteExpiresAt(timeService.nowKst());
            partyRepository.save(party);

            return PartyLeaveResDto.builder()
                    .partyId(party.getId())
                    .dissolved(true)
                    .newHostUserId(null)
                    .build();
        }

        // 승계 대상은 준비완료 대상이 아니므로 대기 상태로 초기화
        PartyMember newHost = remainingMembers.get(0);
        newHost.updateRole(PartyMemberRole.HOST);
        newHost.waiting();
        partyMemberRepository.save(newHost);
        party.updateHostUser(newHost.getUser());
        partyRepository.save(party);

        return PartyLeaveResDto.builder()
                .partyId(party.getId())
                .dissolved(false)
                .newHostUserId(newHost.getUser().getId())
                .build();
    }

    public PartyWaitingResDto updatePartyMemberReady(Long partyId, Long userId, PartyReadyReqDto request) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository.findById(new PartyMemberId(partyId, userId))
                .orElseThrow(() -> new ForbiddenException(ErrorStatus._NOT_PARTY_MEMBER));

        if (member.getRole() == PartyMemberRole.HOST) {
            throw new BusinessRuleException(ErrorStatus._HOST_CANNOT_READY);
        }

        if (request.ready()) {
            User user = member.getUser();
            if (user.getPointBalance() < party.getDepositAmount()) {
                throw new InsufficientPointException(
                        ErrorStatus._INSUFFICIENT_POINT_FOR_PARTY,
                        user.getPointBalance(),
                        party.getDepositAmount()
                );
            }
            member.ready();
        } else {
            member.waiting();
        }

        List<PartyMember> partyMembers = partyMemberRepository.findByParty_IdOrderByJoinedAtAsc(partyId);
        return PartyWaitingResDto.of(party, partyMembers, userId);
    }
}
