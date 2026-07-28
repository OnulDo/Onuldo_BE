package com.example.onuldo.domain.file.controller;

import com.example.onuldo.domain.file.controller.doc.FileControllerDoc;
import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.global.aws.service.RekognitionService;
import com.example.onuldo.global.aws.service.S3FileService;
import com.example.onuldo.global.aws.service.S3FileService.S3ImageFile;
import com.example.onuldo.global.common.base.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileController implements FileControllerDoc {

    private final S3FileService s3FileService;
    private final RekognitionService rekognitionService;

    @PostMapping(name = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<S3UploadResDto> uploadImage(
            @RequestPart("file")
            MultipartFile file
    ) {
        return BaseResponse.onSuccess("이미지 업로드에 성공했습니다.", s3FileService.uploadImage(file));
    }

    @GetMapping("/images/view")
    public ResponseEntity<StreamingResponseBody> getImage(
            @RequestParam("fileId")
            String fileId
    ) {
        S3ImageFile image = s3FileService.getImage(fileId);
        StreamingResponseBody body = outputStream -> {
            try (var inputStream = image.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff");

        if (image.contentLength() != null && image.contentLength() >= 0) {
            responseBuilder.contentLength(image.contentLength());
        }

        return responseBuilder.body(body);
    }
}
