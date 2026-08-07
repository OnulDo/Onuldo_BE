package com.example.onuldo.domain.party.service;

import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.challenge.repository.VerificationRepository;
import com.example.onuldo.domain.party.dto.response.PartyFeedItemResDto;
import com.example.onuldo.domain.party.dto.response.PartyFeedResDto;
import com.example.onuldo.domain.party.entity.Party;
import com.example.onuldo.domain.party.entity.PartyChallenge;
import com.example.onuldo.domain.party.entity.PartyMember;
import com.example.onuldo.domain.party.repository.PartyChallengeRepository;
import com.example.onuldo.domain.party.repository.PartyMemberRepository;
import com.example.onuldo.domain.party.repository.PartyRepository;
import com.example.onuldo.domain.user.entity.User;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import com.example.onuldo.global.common.time.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyFeedService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyChallengeRepository partyChallengeRepository;
    private final VerificationRepository verificationRepository;
    private final TimeService timeService;

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
                verificationRepository.findTodayAutoPassVerificationsByPartyId(partyId, timeService.todayKst());

        // 파티원 1인당 하루 1회 인증이 원칙이므로 userId 기준으로 매핑 (중복 키 발생 시 첫 번째 인증 건 유지)
        Map<Long, Verification> verificationByUserId = todayVerifications.stream()
                .collect(Collectors.toMap(
                        v -> v.getParticipation().getUser().getId(),
                        v -> v,
                        (first, second) -> first
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
