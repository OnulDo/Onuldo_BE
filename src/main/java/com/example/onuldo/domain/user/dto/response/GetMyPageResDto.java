package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetMyPageResDto(
        String nickname,
        String email,
        String profileImageUrl,
        Long currentPoint,
        LocalDate joinedAt
) {
}
