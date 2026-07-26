package com.example.onuldo.domain.challenge.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ParticipationResDto(
        LocalDate startDate,
        LocalDate endDate,
        Integer durationWeeks,
        Integer durationDays,
        Integer depositAmount,
        Integer expectedRefundAmount
) {
}
