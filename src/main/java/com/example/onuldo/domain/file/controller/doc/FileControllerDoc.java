package com.example.onuldo.domain.file.controller.doc;

import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "file", description = "파일 업로드 관련 API")
public interface FileControllerDoc {

    @Operation(
            summary = "이미지 S3 업로드",
            description = "multipart 이미지 파일을 S3에 업로드하고 저장 위치를 반환합니다."
    )
    BaseResponse<S3UploadResDto> uploadImage(
            @RequestPart("file")
            MultipartFile file
    );
}
