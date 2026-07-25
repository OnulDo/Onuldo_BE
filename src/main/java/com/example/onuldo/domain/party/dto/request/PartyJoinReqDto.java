package com.example.onuldo.domain.party.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PartyJoinReqDto(
        @NotBlank(message = "초대코드는 필수입니다.")
        String inviteCode
) {
}
