package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.challenge.entity.Verification;
import com.example.onuldo.domain.user.entity.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartyFeedItemResDto(
        Long userId,
        String nickname,
        String profileImageUrl,
        boolean isVerifiedToday,
        String verificationPhotoUrl,
        LocalDateTime verifiedAt
) {
    public static PartyFeedItemResDto verified(User user, Verification verification) {
        return PartyFeedItemResDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .isVerifiedToday(true)
                .verificationPhotoUrl(verification.getPhotoUrl())
                .verifiedAt(verification.getVerifiedAt())
                .build();
    }

    public static PartyFeedItemResDto notVerified(User user) {
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
