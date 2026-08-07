package com.example.onuldo.global.aws.service;

import com.example.onuldo.domain.file.dto.S3UploadResDto;
import com.example.onuldo.domain.file.dto.S3FileUrlResDto;
import com.example.onuldo.global.aws.config.AwsProperties;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.exception.SdkException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import com.example.onuldo.global.common.time.TimeService;

@Service
@RequiredArgsConstructor
public class S3FileService {
    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final TimeService timeService;

    public S3UploadResDto uploadImage(MultipartFile file) {
        ImageUploadPayload payload = readAndValidateImage(file);
        String bucket = resolveBucket();
        String key = createObjectKey(payload.contentType());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(payload.contentType())
                            .contentLength((long) payload.bytes().length)
                            .build(),
                    RequestBody.fromBytes(payload.bytes())
            );
        } catch (RuntimeException e) {
            throw new RestApiException(GlobalErrorStatus._S3_UPLOAD_FAILED);
        }

        return S3UploadResDto.builder()
                .bucket(bucket)
                .fileId(key)
                .url(createFileUrl(key))
                .contentType(payload.contentType())
                .size((long) payload.bytes().length)
                .build();
    }

    public S3FileUrlResDto getFileUrl(String fileId) {
        String bucket = resolveBucket();
        validateFileId(fileId);
        verifyObjectExists(bucket, fileId);

        return S3FileUrlResDto.builder()
                .bucket(bucket)
                .fileId(fileId)
                .url(createAccessUrl(bucket, fileId))
                .build();
    }

    private ImageUploadPayload readAndValidateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RestApiException(GlobalErrorStatus._IMAGE_FILE_REQUIRED);
        }

        String contentType = normalizeImageContentType(file.getContentType());
        if (contentType == null) {
            throw new RestApiException(GlobalErrorStatus._UNSUPPORTED_IMAGE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            boolean jpeg = isJpeg(bytes);
            boolean png = isPng(bytes);

            if (!jpeg && !png) {
                throw new RestApiException(GlobalErrorStatus._INVALID_IMAGE_FORMAT);
            }

            if (jpeg && !"image/jpeg".equals(contentType)) {
                throw new RestApiException(GlobalErrorStatus._JPEG_CONTENT_TYPE_MISMATCH);
            }

            if (png && !"image/png".equals(contentType)) {
                throw new RestApiException(GlobalErrorStatus._PNG_CONTENT_TYPE_MISMATCH);
            }

            return new ImageUploadPayload(contentType, bytes);
        } catch (IOException e) {
            throw new RestApiException(GlobalErrorStatus._IMAGE_READ_FAILED);
        }
    }

    private String resolveBucket() {
        String bucket = awsProperties.s3().bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._S3_BUCKET_NOT_CONFIGURED);
        }

        return bucket;
    }

    private void validateFileId(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._FILE_ID_REQUIRED);
        }
    }

    private void verifyObjectExists(String bucket, String fileId) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileId)
                            .build()
            );
        } catch (NoSuchKeyException e) {
            throw new RestApiException(GlobalErrorStatus._FILE_NOT_FOUND);
        } catch (S3Exception e) {
            if (e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new RestApiException(GlobalErrorStatus._FILE_NOT_FOUND);
            }
            throw new RestApiException(GlobalErrorStatus._FILE_EXISTENCE_CHECK_FAILED);
        } catch (SdkException | IllegalArgumentException e) {
            throw new RestApiException(GlobalErrorStatus._FILE_EXISTENCE_CHECK_FAILED);
        }
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "uploads";
        }

        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String createFileUrl(String key) {
        return createAccessUrl(resolveBucket(), key);
    }

    private String createAccessUrl(String bucket, String key) {
        String publicBaseUrl = awsProperties.s3().publicBaseUrl();
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._S3_PUBLIC_BASE_URL_NOT_CONFIGURED);
        }

        return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
    }

    private String createObjectKey(String contentType) {
        LocalDate today = timeService.todayKst();
        String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
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

    private String normalizeImageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        return switch (contentType.trim().toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "image/jpeg";
            case "image/png" -> "image/png";
            default -> null;
        };
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes != null
                && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes != null
                && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E
                && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D
                && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0x0A;
    }

    private record ImageUploadPayload(String contentType, byte[] bytes) {
    }
}
