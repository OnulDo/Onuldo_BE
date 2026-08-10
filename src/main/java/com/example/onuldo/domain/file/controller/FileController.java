package com.example.onuldo.domain.file.controller;

import com.example.onuldo.domain.file.controller.doc.FileControllerDoc;
import com.example.onuldo.domain.file.dto.S3FileUrlResDto;
import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.global.aws.service.S3FileService;
import com.example.onuldo.global.common.base.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController implements FileControllerDoc {

    private final S3FileService s3FileService;

    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<S3UploadResDto> uploadImage(
            @RequestPart("file")
            MultipartFile file
    ) {
        return BaseResponse.onSuccess("이미지 업로드에 성공했습니다.", s3FileService.uploadImage(file));
    }

    @GetMapping("/url")
    public BaseResponse<S3FileUrlResDto> getFileUrl(
            @RequestParam("fileId")
            String fileId
    ) {
        return BaseResponse.onSuccess("파일 접근 URL 조회에 성공했습니다.", s3FileService.getFileUrl(fileId));
    }
}
