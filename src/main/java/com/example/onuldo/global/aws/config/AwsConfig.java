package com.example.onuldo.global.aws.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        return S3Client.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(credentialsProviderOrDefault(awsProperties))
                .build();
    }

    @Bean
    public RekognitionClient rekognitionClient(AwsProperties awsProperties) {
        return RekognitionClient.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(credentialsProviderOrDefault(awsProperties))
                .build();
    }

    private StaticCredentialsProvider credentialsProvider(AwsProperties awsProperties) {
        AwsProperties.Credentials credentials = awsProperties.credentials();
        if (credentials == null
                || credentials.accessKeyId() == null || credentials.accessKeyId().isBlank()
                || credentials.secretAccessKey() == null || credentials.secretAccessKey().isBlank()) {
            return null;
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(credentials.accessKeyId(), credentials.secretAccessKey())
        );
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentialsProviderOrDefault(
            AwsProperties awsProperties
    ) {
        StaticCredentialsProvider credentialsProvider = credentialsProvider(awsProperties);
        return credentialsProvider != null ? credentialsProvider : DefaultCredentialsProvider.create();
    }
}
