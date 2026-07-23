package com.example.onuldo.domain.party.dto.response;

import com.example.onuldo.domain.party.enums.PartyStatus;
import lombok.Builder;

@Builder
public record PartyListResDto(
        Long partyId,
        String name,
        PartyStatus status,
        Integer dDay,
        Double progressRate,
        Integer verifiedToday,
        Integer totalMembers
) {
    // JPQL "new" 생성자 프로젝션 또는 QueryDSL Projections.constructor에서
    // 이 record의 canonical constructor를 그대로 사용
}
