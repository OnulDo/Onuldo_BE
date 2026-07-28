package com.example.onuldo.global.aws.service;

import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.global.aws.config.AwsProperties;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public S3UploadResDto uploadImage(MultipartFile file) {
        validateImageFile(file);

        String bucket = resolveBucket();
        String key = createObjectKey(file.getOriginalFilename());
        String contentType = resolveContentType(file);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "S3 업로드 파일을 읽는 중 오류가 발생했습니다.");
        } catch (RuntimeException e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "S3 업로드에 실패했습니다.");
        }

        return S3UploadResDto.builder()
                .bucket(bucket)
                .fileId(key)
                .url(createFileUrl(bucket, key))
                .contentType(contentType)
                .size(file.getSize())
                .build();
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "업로드할 이미지 파일이 필요합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String resolveBucket() {
        String bucket = awsProperties.s3().bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "AWS S3 버킷 설정이 필요합니다.");
        }

        return bucket;
    }

    private String createObjectKey(String originalFilename) {
        LocalDate today = LocalDate.now();
        String extension = extractExtension(originalFilename);
        String prefix = normalizePrefix(awsProperties.s3().uploadPrefix());

        return "%s/%04d/%02d/%02d/%s%s".formatted(
                prefix,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex).toLowerCase();
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "uploads";
        }

        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? DEFAULT_CONTENT_TYPE : contentType;
    }

    private String createFileUrl(String bucket, String key) {
        String publicBaseUrl = awsProperties.s3().publicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, awsProperties.region(), key);
    }
}
