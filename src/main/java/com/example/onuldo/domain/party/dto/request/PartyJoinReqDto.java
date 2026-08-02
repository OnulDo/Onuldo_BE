package com.example.onuldo.domain.party.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyJoinReqDto(
        @Schema(example = "82K3H9")
        @NotBlank(message = "초대코드는 필수입니다.")
        String inviteCode
) {
}
