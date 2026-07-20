package com.example.onuldo.domain.challenge.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ChallengePageResDto(
        List<ChallengeResDto> challenges,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
