package com.my.challenger.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.photo.Photo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3PhotoStorageService {

    private final S3Client s3Client;
    private final PhotoRepository photoRepository;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${app.s3.region}")
    private String region;

    @Value("${app.s3.cloudfront-domain:}")
    private String cloudFrontDomain;

    @Value("${app.upload.max-file-size:10485760}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final int AVATAR_SIZE = 400;
    private static final int MAX_WIDTH = 2048;
    private static final int MAX_HEIGHT = 2048;

    public Photo savePhoto(MultipartFile file, PhotoType photoType, Long entityId, Long uploadedBy)
            throws IOException {

        validateFile(file);

        // Generate S3 key (path)
        String s3Key = generateS3Key(photoType, file.getOriginalFilename());

        // Process image
        byte[] processedImage = processImage(file, photoType);
        Dimension dimensions = getImageDimensions(processedImage);

        // Build metadata
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("original-filename", file.getOriginalFilename());
        userMetadata.put("photo-type", photoType.name());
        userMetadata.put("uploaded-by", uploadedBy.toString());

        // Create PutObjectRequest
        PutObjectRequest.Builder putRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .cacheControl("public, max-age=31536000")
                .metadata(userMetadata);

        // Make public readable for avatars and quiz images
        if (photoType == PhotoType.AVATAR || photoType == PhotoType.QUIZ_QUESTION) {
            putRequestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        PutObjectRequest putRequest = putRequestBuilder.build();

        // Upload to S3
        s3Client.putObject(putRequest, RequestBody.fromBytes(processedImage));

        // Save to database
        Photo photo = Photo.builder()
                .filename(extractFilenameFromKey(s3Key))
                .originalFilename(file.getOriginalFilename())
                .filePath(s3Key) // Store S3 key as file path
                .fileSize((long) processedImage.length)
                .mimeType(file.getContentType())
                .width(dimensions.width)
                .height(dimensions.height)
                .uploadedBy(uploadedBy)
                .photoType(photoType)
                .entityId(entityId)
                .build();

        // Remove old photo if updating avatar
        if (photoType == PhotoType.AVATAR && entityId != null) {
            deleteExistingPhoto(entityId, photoType);
        }

        return photoRepository.save(photo);
    }

    private String generateS3Key(PhotoType photoType, String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String randomId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);

        return String.format("%s/%s/%s.%s",
                photoType.name().toLowerCase(),
                timestamp,
                randomId,
                extension
        );
    }

    private String extractFilenameFromKey(String s3Key) {
        return s3Key.substring(s3Key.lastIndexOf('/') + 1);
    }

    public byte[] getPhotoData(Photo photo) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(photo.getFilePath())
                    .build();

            return s3Client.getObject(getObjectRequest).readAllBytes();
        } catch (Exception e) {
            throw new IOException("Failed to retrieve photo from S3", e);
        }
    }

    public String getPhotoUrl(Photo photo) {
        if (!cloudFrontDomain.isEmpty()) {
            // Use CloudFront for better performance
            return String.format("https://%s/%s", cloudFrontDomain, photo.getFilePath());
        } else {
            // Use direct S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, photo.getFilePath());
        }
    }

    public void deletePhoto(Long photoId) throws IOException {
        Optional<Photo> photoOpt = photoRepository.findById(photoId);
        if (photoOpt.isPresent()) {
            Photo photo = photoOpt.get();

            // Delete from S3
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(photo.getFilePath())
                        .build();
                s3Client.deleteObject(deleteRequest);
            } catch (Exception e) {
                log.warn("Failed to delete photo from S3: {}", e.getMessage());
            }

            // Delete from database
            photoRepository.delete(photo);
        }
    }

    private void deleteExistingPhoto(Long entityId, PhotoType photoType) {
        photoRepository.findByEntityIdAndPhotoType(entityId, photoType)
                .ifPresent(existingPhoto -> {
                    try {
                        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(existingPhoto.getFilePath())
                                .build();
                        s3Client.deleteObject(deleteRequest);
                        photoRepository.delete(existingPhoto);
                    } catch (Exception e) {
                        log.warn("Failed to delete existing S3 photo: {}", e.getMessage());
                    }
                });
    }

    // Image processing methods remain the same as in PhotoStorageService
    private void validateFile(MultipartFile file) throws IOException {
        // Same validation logic as before
    }

    private byte[] processImage(MultipartFile file, PhotoType photoType) throws IOException {
        // Same image processing logic as before
    }

    private BufferedImage resizeToSquare(BufferedImage originalImage, int size) {
        // Same resize logic as before
    }

    private BufferedImage resizeIfNeeded(BufferedImage originalImage, int maxWidth, int maxHeight) {
        // Same resize logic as before
    }

    private Dimension getImageDimensions(byte[] imageData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        return new Dimension(image.getWidth(), image.getHeight());
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}