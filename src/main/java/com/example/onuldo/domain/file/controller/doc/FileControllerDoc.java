package com.example.onuldo.domain.file.controller.doc;

import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "file", description = "파일 관련 API")
public interface FileControllerDoc {

    @Operation(
            summary = "이미지 S3 업로드",
            description = "multipart 이미지 파일을 S3에 업로드하고 저장 위치를 반환합니다."
    )
    BaseResponse<S3UploadResDto> uploadImage(
            @RequestPart("file")
            MultipartFile file
    );

    @Operation(
            summary = "이미지 조회",
            description = "fileId에 해당하는 S3 이미지를 브라우저에서 바로 표시할 수 있도록 이미지 바이트로 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "이미지 조회 성공",
            content = @Content(
                    mediaType = "image/*",
                    schema = @Schema(type = "string", format = "binary")
            )
    )
    ResponseEntity<StreamingResponseBody> getImage(
            @Parameter(description = "S3 file Id", example = "uploads/2026/07/28/018fffd1-7fc0-7bdd-9e9e-3c5cc3f2d3a0.jpg")
            @RequestParam("fileId")
            String fileId
    );
}
