package com.example.onuldo.domain.party.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PartyCreateReqDto(
        @Schema(example = "갓생팟")
        @NotBlank(message = "파티 이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "파티 이름은 2~20자여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "파티 이름은 한글, 영문, 숫자만 가능합니다.")
        String name,

        @Schema(example = "12")
        @NotNull(message = "챌린지 ID는 필수입니다.")
        Long challengeId,

        @Schema(example = "4")
        @NotNull(message = "진행 기간은 필수입니다.")
        Integer durationWeeks,

        @Schema(example = "30000")
        @NotNull(message = "도전금은 필수입니다.")
        Integer depositAmount,

        @Schema(example = "4")
        @NotNull(message = "모집 인원은 필수입니다.")
        @Min(value = 2, message = "모집 인원은 최소 2명입니다.")
        @Max(value = 5, message = "모집 인원은 최대 5명입니다.")
        Integer maxMembers
) {
}
