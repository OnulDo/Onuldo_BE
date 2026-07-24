package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record PartyListResDto(
        Long partyId,
        String name,
        String challengeTitle,
        PartyStatus status,
        LocalDate endDate,
        LocalTime verificationDeadline,
        Double progressRate,
        Integer verifiedMemberCount,
        Integer totalMemberCount
) {
    // JPQL "new" 생성자 프로젝션 또는 QueryDSL Projections.constructor에서
    // 이 record의 canonical constructor를 그대로 사용
}
