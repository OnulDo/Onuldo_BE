package com.example.onuldo.global.aws.service;

import com.example.onuldo.domain.file.dto.RekognitionLabelResDto;
import com.example.onuldo.global.aws.config.AwsProperties;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RekognitionService {

    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;

    private final S3Client s3Client;

    public List<RekognitionLabelResDto> detectLabelsByFileId(String bucket, String fileId) {
        if (bucket == null || bucket.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._S3_BUCKET_REQUIRED);
        }
        if (fileId == null || fileId.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._S3_FILE_ID_REQUIRED);
        }

        return detectLabels(bucket, fileId);
    }

    public List<RekognitionLabelResDto> detectLabelsByFileId(String fileId) {
        return detectLabelsByFileId(resolveBucket(), fileId);
    }

    public List<String> detectLabelNamesByFileId(String fileId) {
        return detectLabelsByFileId(fileId).stream()
                .map(RekognitionLabelResDto::name)
                .toList();
    }

    public List<RekognitionLabelResDto> detectLabels(String bucket, String key) {
        try {
            // S3 파일 존재 여부 확인
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            return rekognitionClient.detectLabels(DetectLabelsRequest.builder()
                            .image(Image.builder()
                                    .s3Object(S3Object.builder()
                                            .bucket(bucket)
                                            .name(key)
                                            .build())
                                    .build())
                            .maxLabels(resolveMaxLabels())
                            .minConfidence(resolveMinConfidence())
                            .build())
                    .labels()
                    .stream()
                    .map(label -> RekognitionLabelResDto.builder()
                            .name(label.name())
                            .confidence(label.confidence())
                            .build())
                    .toList();

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new RestApiException(GlobalErrorStatus._FILE_NOT_FOUND);
            }
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, e.getMessage());

        } catch (Exception e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private int resolveMaxLabels() {
        Integer maxLabels = awsProperties.rekognition().maxLabels();
        return maxLabels == null || maxLabels <= 0 ? 10 : maxLabels;
    }

    private float resolveMinConfidence() {
        Float minConfidence = awsProperties.rekognition().minConfidence();
        return minConfidence == null ? 80F : minConfidence;
    }

    private String resolveBucket() {
        String bucket = awsProperties.s3().bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._S3_BUCKET_NOT_CONFIGURED);
        }

        return bucket;
    }
}
