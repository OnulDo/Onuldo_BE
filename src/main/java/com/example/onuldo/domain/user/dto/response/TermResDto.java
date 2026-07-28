package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.auth.enums.TermType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record TermResDto(
        @Schema(description = "약관 종류")
        TermType termType,
        @Schema(description = "약관 제목")
        String title,
        @Schema(description = "시행 일자")
        LocalDate effectiveDate,
        @Schema(description = "약관 내용(json)")
        JsonNode content
) {}
