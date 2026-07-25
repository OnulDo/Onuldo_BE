package com.example.onuldo.domain.challenge.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record UserChallengeListResDto(
        List<UserChallengeResDto> challenges
) {
}
