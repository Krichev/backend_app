package com.my.challenger.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MinIO startup configuration
 * Ensures buckets are created on application startup
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class MinioStartupConfig {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Value("${app.storage.minio.public-bucket:false}")
    private boolean publicBucket;

    @Bean
    public ApplicationRunner initializeMinio() {
        return args -> {
            try {
                log.info("Initializing MinIO buckets...");
                
                List<String> bucketsToCreate = new ArrayList<>();
                StorageProperties.S3.Buckets buckets = storageProperties.getS3().getBuckets();
                
                if (buckets != null) {
                    if (buckets.getImages() != null) bucketsToCreate.add(buckets.getImages());
                    if (buckets.getAudio() != null) bucketsToCreate.add(buckets.getAudio());
                    if (buckets.getVideos() != null) bucketsToCreate.add(buckets.getVideos());
                    if (buckets.getDocuments() != null) bucketsToCreate.add(buckets.getDocuments());
                }
                
                // Add legacy bucket if configured
                String legacyBucket = storageProperties.getS3().getBucketName();
                if (legacyBucket != null && !legacyBucket.isEmpty() && !bucketsToCreate.contains(legacyBucket)) {
                    bucketsToCreate.add(legacyBucket);
                }

                for (String bucketName : bucketsToCreate) {
                    createBucketIfNotExists(bucketName);
                }
                
                log.info("MinIO initialization completed successfully");
            } catch (Exception e) {
                log.warn("MinIO initialization failed, but application will continue: {}", e.getMessage());
            }
        };
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            // Check if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            try {
                s3Client.headBucket(headBucketRequest);
                log.info("Bucket '{}' already exists", bucketName);
            } catch (Exception e) {
                // Bucket doesn't exist, create it
                log.info("Bucket '{}' doesn't exist, creating...", bucketName);
                createBucket(bucketName);
            }

            // Set bucket policy to allow public read access
            setBucketPolicy(bucketName);

        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
            // Don't throw exception to allow other buckets to proceed
        }
    }

    private void createBucket(String bucketName) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();

        s3Client.createBucket(createBucketRequest);
        log.info("Created bucket: {}", bucketName);
    }

    private void setBucketPolicy(String bucketName) {
        if (!publicBucket) {
            log.info("Bucket '{}' is PRIVATE - presigned URLs required", bucketName);
            return; // Don't set public policy
        }

        // Only for development - set public policy
        log.warn("⚠️ Bucket '{}' is PUBLIC - not recommended for production!", bucketName);

        try {
            // Public read policy
            String policy = String.format("""
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": "*"},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """, bucketName);

            PutBucketPolicyRequest policyRequest = PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policy)
                    .build();

            s3Client.putBucketPolicy(policyRequest);
            log.info("Set public bucket policy for: {}", bucketName);

        } catch (Exception e) {
            log.warn("Failed to set bucket policy for '{}' (this is optional): {}", bucketName, e.getMessage());
        }
    }
}
