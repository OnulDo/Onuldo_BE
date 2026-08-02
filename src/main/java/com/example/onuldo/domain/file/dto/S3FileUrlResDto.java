package com.example.onuldo.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record S3FileUrlResDto(
        @Schema(example = "onuldo-bucket")
        String bucket,
        @Schema(example = "uploads/2026/07/29/uuid.jpg")
        String fileId,
        @Schema(example = "https://onuldo-bucket.s3.ap-northeast-2.amazonaws.com/uploads/2026/07/29/uuid.jpg")
        String url
) {
}
