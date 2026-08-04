package com.example.onuldo.domain.challenge.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record DailyCompletedChallengeListResDto (
        List<CompletedPartyResDto> parties,
        List<CompletedChallengeResDto> challenges
){
}
