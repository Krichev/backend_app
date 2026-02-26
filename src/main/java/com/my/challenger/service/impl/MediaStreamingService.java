package com.my.challenger.service.impl;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaStreamingService {

    private final S3Client s3Client;
    private final MediaFileRepository mediaFileRepository;

    @Value("${app.storage.s3.bucket-name}")
    private String defaultBucketName;

    /**
     * Get media file entity by ID
     */
    public MediaFile getMediaFileById(Long mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));
    }

    /**
     * Get media file entity by S3 key
     */
    public MediaFile getMediaFileByS3Key(String s3Key) {
        return mediaFileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found for key: " + s3Key));
    }

    /**
     * Stream full file from S3/MinIO
     */
    public InputStream streamMedia(String s3Key) {
        try {
            String bucket = resolveBucket(s3Key);
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.error("S3 key not found: {}", s3Key);
            throw new ResourceNotFoundException("Media file not found in storage");
        } catch (Exception e) {
            log.error("Failed to stream from S3: {}", s3Key, e);
            throw new MediaProcessingException("Failed to stream media", e);
        }
    }

    /**
     * Stream partial content (for Range requests)
     */
    public ResponseInputStream<GetObjectResponse> streamMediaRange(String s3Key, String rangeHeader) {
        try {
            String bucket = resolveBucket(s3Key);
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .range(rangeHeader)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.error("S3 key not found: {}", s3Key);
            throw new ResourceNotFoundException("Media file not found in storage");
        } catch (Exception e) {
            log.error("Failed to stream range from S3: {}", s3Key, e);
            throw new MediaProcessingException("Failed to stream media", e);
        }
    }

    /**
     * Get file metadata from S3 (for Content-Length, ETag)
     */
    public HeadObjectResponse getObjectMetadata(String s3Key) {
        try {
            String bucket = resolveBucket(s3Key);
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            return s3Client.headObject(request);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Media file not found in storage");
        } catch (Exception e) {
            log.error("Failed to get S3 metadata: {}", s3Key, e);
            throw new MediaProcessingException("Failed to get media metadata", e);
        }
    }

    /**
     * Resolves the bucket name for a given S3 key by looking up the MediaFile record.
     * Falls back to the default bucket name if no record is found.
     */
    private String resolveBucket(String s3Key) {
        return mediaFileRepository.findByS3Key(s3Key)
                .map(MediaFile::getBucketName)
                .orElse(defaultBucketName);
    }
}
