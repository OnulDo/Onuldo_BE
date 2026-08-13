package com.example.onuldo.domain.file.controller.doc;

import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.domain.file.dto.S3FileUrlResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.config.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "파일 관련 API")
public interface FileControllerDoc {

    @Operation(
            summary = "이미지 S3 업로드",
            description = "multipart 이미지 파일을 S3에 업로드하고 저장 위치를 반환합니다."
    )
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._IMAGE_FILE_REQUIRED,
            ErrorStatus._UNSUPPORTED_IMAGE_TYPE,
            ErrorStatus._INVALID_IMAGE_FORMAT,
            ErrorStatus._JPEG_CONTENT_TYPE_MISMATCH,
            ErrorStatus._PNG_CONTENT_TYPE_MISMATCH,
            ErrorStatus._IMAGE_READ_FAILED,
            ErrorStatus._S3_BUCKET_NOT_CONFIGURED,
            ErrorStatus._S3_UPLOAD_FAILED
    })
    BaseResponse<S3UploadResDto> uploadImage(
            @RequestPart("file")
            MultipartFile file
    );

    @Operation(
            summary = "파일 접근 URL 조회",
            description = "fileId로 S3 접근 URL을 만들고, 존재하지 않으면 404를 반환합니다."
    )
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._FILE_ID_REQUIRED,
            ErrorStatus._INVALID_FILE_URL,
            ErrorStatus._FILE_NOT_FOUND,
            ErrorStatus._FILE_EXISTENCE_CHECK_FAILED,
            ErrorStatus._S3_PUBLIC_BASE_URL_NOT_CONFIGURED
    })
    BaseResponse<S3FileUrlResDto> getFileUrl(
            @Parameter(description = "S3 file Id", example = "uploads/2026/07/29/uuid.jpg")
            @RequestParam("fileId")
            String fileId
    );
}
