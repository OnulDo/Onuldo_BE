package com.example.onuldo.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record S3UploadResDto(
        @Schema(example = "onuldo-bucket")
        String bucket,
        @Schema(example = "uploads/2026/07/28/018fffd1-7fc0-7bdd-9e9e-3c5cc3f2d3a0.jpg", description = "S3 file Id")
        String fileId,
        @Schema(
                example = "/api/file/images/view?fileId=uploads%2F2026%2F07%2F28%2F018fffd1-7fc0-7bdd-9e9e-3c5cc3f2d3a0.jpg",
                description = "브라우저에서 이미지를 바로 조회할 수 있는 URL"
        )
        String url,
        @Schema(example = "image/jpeg")
        String contentType,
        @Schema(example = "240192")
        Long size
) {
}
