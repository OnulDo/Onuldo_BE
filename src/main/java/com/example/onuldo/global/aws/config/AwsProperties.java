package com.example.onuldo.global.aws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        Credentials credentials,
        S3 s3,
        Rekognition rekognition
) {
    public record Credentials(
            String accessKeyId,
            String secretAccessKey
    ) {
    }

    public record S3(
            String bucket,
            String publicBaseUrl,
            String uploadPrefix
    ) {
    }

    public record Rekognition(
            Integer maxLabels,
            Float minConfidence
    ) {
    }
}
