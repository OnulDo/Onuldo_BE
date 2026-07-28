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

import java.util.List;

@Service
@RequiredArgsConstructor
public class RekognitionService {

    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;

    public List<RekognitionLabelResDto> detectLabelsByFileId(String bucket, String fileId) {
        if (bucket == null || bucket.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "S3 bucket 정보가 필요합니다.");
        }
        if (fileId == null || fileId.isBlank()) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "S3 file Id가 필요합니다.");
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
        } catch (RuntimeException e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "AWS Rekognition 객체 감지에 실패했습니다.");
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
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "AWS S3 버킷 설정이 필요합니다.");
        }

        return bucket;
    }
}
