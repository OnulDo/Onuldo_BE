package com.example.onuldo.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PartyHomeResDto(
        @Schema(description = "아직 확인하지 않은 정산 완료 배너 목록. 없으면 빈 배열")
        List<PartySettlementBannerResDto> settlementBanners,
        List<PartyHomeItemResDto> parties
) {
}
