package com.example.onuldo.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RekognitionLabelResDto(
        @Schema(example = "Book")
        String name,
        @Schema(example = "98.45")
        Float confidence
) {
}
