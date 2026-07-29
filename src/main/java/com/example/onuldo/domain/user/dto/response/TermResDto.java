package com.example.onuldo.domain.user.dto.response;

import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.global.dto.response.ContentBlockResDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record TermResDto(
        @Schema(example = "SERVICE")
        TermType termType,
        @Schema(example = "서비스 이용 약관")
        String title,
        @Schema(example = "SERVICE")
        LocalDate effectiveDate,
        @Schema(example = "[{\"type\":\"h2\",\"content\":\"서비스 이용 시 회원가입은 무조건 해야합니다.\"}]")
        List<ContentBlockResDto> content
) {}
