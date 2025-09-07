package com.my.challenger.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3Config {

    @Value("${app.s3.access-key}")
    private String accessKey;

    @Value("${app.s3.secret-key}")
    private String secretKey;

    @Value("${app.s3.region:us-east-1}")
    private String region;

    @Value("${app.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        // For LocalStack or custom S3-compatible services
        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true); // Required for LocalStack
        }

        return builder.build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        // For LocalStack or custom S3-compatible services
        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        // For LocalStack or custom S3-compatible services
        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}