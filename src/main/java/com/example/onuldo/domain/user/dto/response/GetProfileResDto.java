package com.example.onuldo.domain.user.dto.response;

import lombok.Builder;

@Builder
public record GetProfileResDto(
        String nickname,
        String email,
        String profileImageUrl
) {
}
