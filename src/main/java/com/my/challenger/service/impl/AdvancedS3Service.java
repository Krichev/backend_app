package com.my.challenger.service.impl;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import com.my.challenger.entity.MediaFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class AdvancedS3Service {

    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    
    @Value("${app.s3.bucket-name}")
    private String bucketName;

    /**
     * Generate pre-signed URL for direct browser upload
     */
    public String generateUploadPresignedUrl(String key, String contentType, int expirationMinutes) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * Generate pre-signed URL for private media download
     */
    public String generateDownloadPresignedUrl(String key, int expirationMinutes) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Async copy operation for creating thumbnails or duplicates
     */
    public CompletableFuture<String> copyObjectAsync(String sourceKey, String destinationKey) {
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build();

        return s3AsyncClient.copyObject(copyRequest)
                .thenApply(response -> {
                    log.info("Successfully copied {} to {}", sourceKey, destinationKey);
                    return destinationKey;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to copy {} to {}: {}", sourceKey, destinationKey, throwable.getMessage());
                    throw new RuntimeException("Copy operation failed", throwable);
                });
    }

    /**
     * Check if object exists
     */
    public boolean objectExists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("Error checking if object exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get object metadata without downloading
     */
    public ObjectMetadata getObjectMetadata(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(headRequest);
            
            return ObjectMetadata.builder()
                    .contentLength(response.contentLength())
                    .contentType(response.contentType())
                    .lastModified(response.lastModified())
                    .metadata(response.metadata())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get object metadata for key: {}", key, e);
            throw new RuntimeException("Failed to get metadata", e);
        }
    }

    /**
     * Async delete with error handling
     */
    public CompletableFuture<Void> deleteObjectAsync(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3AsyncClient.deleteObject(deleteRequest)
                .thenAccept(response -> log.info("Successfully deleted object: {}", key))
                .exceptionally(throwable -> {
                    log.error("Failed to delete object {}: {}", key, throwable.getMessage());
                    return null;
                });
    }

    /**
     * Set object ACL (Access Control List)
     */
    public void setObjectAcl(String key, ObjectCannedACL acl) {
        PutObjectAclRequest aclRequest = PutObjectAclRequest.builder()
                .bucket(bucketName)
                .key(key)
                .acl(acl)
                .build();

        s3Client.putObjectAcl(aclRequest);
        log.info("Set ACL {} for object: {}", acl, key);
    }

    /**
     * Create bucket if it doesn't exist
     */
    public void createBucketIfNotExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headRequest);
            log.info("Bucket {} already exists", bucketName);
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                
                s3Client.createBucket(createRequest);
                log.info("Created bucket: {}", bucketName);
            } catch (Exception createException) {
                log.error("Failed to create bucket: {}", bucketName, createException);
                throw new RuntimeException("Failed to create bucket", createException);
            }
        } catch (Exception e) {
            log.error("Error checking bucket existence: {}", e.getMessage());
            throw new RuntimeException("Error checking bucket", e);
        }
    }
}

// Helper class for object metadata
@lombok.Data
@lombok.Builder
class ObjectMetadata {
    private Long contentLength;
    private String contentType;
    private java.time.Instant lastModified;
    private java.util.Map<String, String> metadata;
}