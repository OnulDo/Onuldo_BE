package com.example.onuldo.domain.party.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PartyReadyReqDto(
        @Schema(example = "true")
        @NotNull(message = "준비완료 여부는 필수입니다.")
        Boolean ready
) {
}
