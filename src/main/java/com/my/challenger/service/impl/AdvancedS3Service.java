// File: AdvancedS3Service.java
package com.my.challenger.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
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

    @Value("${app.s3.region:us-east-1}")
    private String region;

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
     * Upload file to S3
     */
    public String uploadFile(String key, byte[] content, String contentType, Map<String, String> metadata) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType);

        if (metadata != null && !metadata.isEmpty()) {
            requestBuilder.metadata(metadata);
        }

        PutObjectRequest request = requestBuilder.build();

        s3Client.putObject(request, RequestBody.fromBytes(content));

        log.info("Successfully uploaded file to S3: {}", key);
        return key;
    }

    /**
     * Download file from S3
     */
    public byte[] downloadFile(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(getRequest, ResponseTransformer.toBytes()).asByteArray();
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Successfully deleted file from S3: {}", key);
    }

    /**
     * Async upload operation
     */
    public CompletableFuture<String> uploadFileAsync(String key, byte[] content, String contentType, Map<String, String> metadata) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType);

        if (metadata != null && !metadata.isEmpty()) {
            requestBuilder.metadata(metadata);
        }

        PutObjectRequest request = requestBuilder.build();

        return s3AsyncClient.putObject(request, software.amazon.awssdk.core.async.AsyncRequestBody.fromBytes(content))
                .thenApply(response -> {
                    log.info("Successfully uploaded file to S3 async: {}", key);
                    return key;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to upload file to S3: {}", key, throwable);
                    throw new RuntimeException("Upload failed", throwable);
                });
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
    public S3ObjectMetadata getObjectMetadata(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);

            return S3ObjectMetadata.builder()
                    .contentLength(response.contentLength())
                    .contentType(response.contentType())
                    .lastModified(response.lastModified())
                    .metadata(response.metadata())
                    .eTag(response.eTag())
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
                CreateBucketRequest.Builder createRequestBuilder = CreateBucketRequest.builder()
                        .bucket(bucketName);

                // Only specify location constraint for regions other than us-east-1
                if (!"us-east-1".equals(region)) {
                    CreateBucketConfiguration configuration = CreateBucketConfiguration.builder()
                            .locationConstraint(BucketLocationConstraint.fromValue(region))
                            .build();
                    createRequestBuilder.createBucketConfiguration(configuration);
                }

                CreateBucketRequest createRequest = createRequestBuilder.build();

                s3Client.createBucket(createRequest);
                log.info("Created bucket: {}", bucketName);

                // Wait for bucket to be available
                s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build());

                // Set bucket versioning (optional)
                setBucketVersioning(true);

            } catch (Exception createException) {
                log.error("Failed to create bucket: {}", bucketName, createException);
                throw new RuntimeException("Failed to create bucket", createException);
            }
        } catch (Exception e) {
            log.error("Error checking bucket existence: {}", e.getMessage());
            throw new RuntimeException("Error checking bucket", e);
        }
    }

    /**
     * Enable or disable bucket versioning
     */
    public void setBucketVersioning(boolean enabled) {
        try {
            BucketVersioningStatus status = enabled ?
                    BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED;

            PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(status)
                            .build())
                    .build();

            s3Client.putBucketVersioning(versioningRequest);
            log.info("Set bucket versioning to {} for bucket: {}", status, bucketName);
        } catch (Exception e) {
            log.warn("Failed to set bucket versioning: {}", e.getMessage());
        }
    }

    /**
     * List objects in bucket with prefix
     */
    public java.util.List<S3Object> listObjects(String prefix, int maxKeys) {
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(maxKeys);

        if (prefix != null && !prefix.isEmpty()) {
            requestBuilder.prefix(prefix);
        }

        ListObjectsV2Request request = requestBuilder.build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents();
    }

    /**
     * Move object (copy and delete)
     */
    public void moveObject(String sourceKey, String destinationKey) {
        // Copy object
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .build();

        s3Client.copyObject(copyRequest);

        // Delete original
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(sourceKey)
                .build();

        s3Client.deleteObject(deleteRequest);

        log.info("Successfully moved object from {} to {}", sourceKey, destinationKey);
    }

    /**
     * Get public URL for object (only works if bucket/object is public)
     */
    public String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    // Helper class for object metadata
    @lombok.Data
    @lombok.Builder
    public static class S3ObjectMetadata {
        private Long contentLength;
        private String contentType;
        private java.time.Instant lastModified;
        private java.util.Map<String, String> metadata;
        private String eTag;
    }
}