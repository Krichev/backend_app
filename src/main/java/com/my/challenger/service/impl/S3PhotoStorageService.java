package com.my.challenger.service.impl;

import com.my.challenger.entity.Photo;
import com.my.challenger.entity.enums.PhotoType;
import com.my.challenger.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Optional;

@Service
//@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3PhotoStorageService implements PhotoStorageService {

    private final S3Client s3Client;
    private final PhotoRepository photoRepository;

    // Optional metrics - will be null if disabled
    @Autowired(required = false)
    private PhotoMetricsService metricsService;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${app.storage.s3.region}")
    private String region;

    @Value("${app.storage.s3.cloudfront-domain:}")
    private String cloudFrontDomain;

    @Value("${app.storage.s3.upload.max-file-size:10485760}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final int MAX_WIDTH = 2048;
    private static final int MAX_HEIGHT = 2048;

    @Override
    public Photo savePhoto(MultipartFile file, PhotoType photoType, Long entityId, Long uploadedBy)
            throws IOException {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting photo upload - Type: {}, EntityId: {}, UploadedBy: {}",
                    photoType, entityId, uploadedBy);

            validateFile(file);
            String s3Key = generateS3Key(photoType, file.getOriginalFilename());
            byte[] processedImage = processImage(file, photoType);
            Dimension dimensions = getImageDimensions(processedImage);

            // Upload to S3
            uploadToS3(s3Key, processedImage, file.getContentType(), photoType, entityId, uploadedBy);

            // Save to database
            Photo photo = createPhotoEntity(s3Key, file, processedImage, dimensions,
                    photoType, entityId, uploadedBy);

            // Handle single-instance photo types (replace existing)
            if ((photoType == PhotoType.AVATAR || photoType == PhotoType.CHALLENGE_COVER) && entityId != null) {
                deleteExistingPhoto(entityId, photoType);
            }

            Photo savedPhoto = photoRepository.save(photo);

            // Record successful upload metrics
            long duration = System.currentTimeMillis() - startTime;
            recordSuccessMetrics(processedImage.length, duration);

            log.info("Successfully uploaded photo with ID: {}", savedPhoto.getId());
            return savedPhoto;

        } catch (IOException e) {
            recordFailureMetrics(e.getMessage());
            throw e;
        } catch (Exception e) {
            recordFailureMetrics("Unexpected error: " + e.getMessage());
            throw new IOException("Unexpected error during photo upload", e);
        }
    }

    @Override
    public void deletePhoto(Long photoId) throws IOException {
        Optional<Photo> photoOpt = photoRepository.findById(photoId);
        if (photoOpt.isPresent()) {
            Photo photo = photoOpt.get();
            long fileSize = photo.getFileSize();

            // Delete from S3
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(photo.getFilePath())
                        .build();
                s3Client.deleteObject(deleteRequest);
                log.info("Successfully deleted photo from S3: {}", photo.getFilename());
            } catch (Exception e) {
                log.warn("Failed to delete photo from S3: {}", e.getMessage());
            }

            // Delete from database
            photoRepository.delete(photo);

            // Record deletion metrics
            recordDeletionMetrics(fileSize);

            log.info("Successfully deleted photo from database: {}", photoId);
        } else {
            log.warn("Photo not found for deletion: {}", photoId);
        }
    }

    @Override
    public byte[] getPhotoData(Photo photo) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(photo.getFilePath())
                    .build();
            return s3Client.getObject(getObjectRequest).readAllBytes();
        } catch (Exception e) {
            log.error("Failed to retrieve photo from S3: {}", e.getMessage());
            throw new IOException("Failed to retrieve photo from S3", e);
        }
    }

    @Override
    public String getPhotoUrl(Photo photo) {
        if (photo.getS3Url() != null && !photo.getS3Url().isEmpty()) {
            return photo.getS3Url();
        }
        return generateS3Url(photo.getFilePath());
    }

    @Override
    public Optional<Photo> getPhoto(Long photoId) {
        return photoRepository.findById(photoId);
    }

    @Override
    public Optional<Photo> getPhotoByEntityAndType(Long entityId, PhotoType photoType) {
        return photoRepository.findByEntityIdAndPhotoType(entityId, photoType);
    }

    @Override
    public List<Photo> getPhotosByEntity(Long entityId) {
        return photoRepository.findByEntityId(entityId);
    }

    @Override
    public List<Photo> getPhotosByUser(Long userId) {
        return photoRepository.findByUploadedBy(userId);
    }

    @Override
    public Optional<Photo> updatePhotoMetadata(Long photoId, String altText, String description) {
        return photoRepository.findById(photoId)
                .map(photo -> {
                    photo.setAltText(altText);
                    photo.setDescription(description);
                    return photoRepository.save(photo);
                });
    }

    @Override
    public boolean photoExists(Long photoId) {
        return photoRepository.existsById(photoId);
    }

    @Override
    public StorageStats getStorageStats() {
        long totalPhotos = photoRepository.count();
        return new StorageStats(totalPhotos, 0L, 0L);
    }

    // Metrics helper methods - safe to call even when metricsService is null

    private void recordSuccessMetrics(long fileSize, long duration) {
        if (metricsService != null) {
            metricsService.recordSuccessfulUpload(fileSize, duration);
        }
    }

    private void recordFailureMetrics(String reason) {
        if (metricsService != null) {
            metricsService.recordFailedUpload(reason);
        }
    }

    private void recordDeletionMetrics(long fileSize) {
        if (metricsService != null) {
            metricsService.recordDeletion(fileSize);
        }
    }

    // Private helper methods (unchanged from original)

    private void uploadToS3(String s3Key, byte[] data, String contentType,
                            PhotoType photoType, Long entityId, Long uploadedBy) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("photo-type", photoType.name());
        metadata.put("uploaded-by", uploadedBy.toString());
        metadata.put("entity-id", entityId != null ? entityId.toString() : "");

        PutObjectRequest.Builder putRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .metadata(metadata);

        if (photoType.isPublicAccessible()) {
            putRequestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        s3Client.putObject(putRequestBuilder.build(), RequestBody.fromBytes(data));
    }

    private Photo createPhotoEntity(String s3Key, MultipartFile file, byte[] processedImage,
                                    Dimension dimensions, PhotoType photoType,
                                    Long entityId, Long uploadedBy) {
        return Photo.builder()
                .filename(extractFilenameFromKey(s3Key))
                .originalFilename(file.getOriginalFilename())
                .filePath(s3Key)
                .fileSize((long) processedImage.length)
                .mimeType(file.getContentType())
                .width(dimensions.width)
                .height(dimensions.height)
                .uploadedBy(uploadedBy)
                .photoType(photoType)
                .entityId(entityId)
                .s3Key(s3Key)
                .s3Url(generateS3Url(s3Key))
                .processingStatus("COMPLETED")
                .build();
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
                        recordDeletionMetrics(existingPhoto.getFileSize());
                    } catch (Exception e) {
                        log.warn("Failed to delete existing photo: {}", e.getMessage());
                    }
                });
    }

    private String generateS3Key(PhotoType photoType, String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String randomId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        return String.format("%s/%s/%s.%s", photoType.name().toLowerCase(), timestamp, randomId, extension);
    }

    private String extractFilenameFromKey(String s3Key) {
        return s3Key.substring(s3Key.lastIndexOf('/') + 1);
    }

    private String generateS3Url(String s3Key) {
        if (!cloudFrontDomain.isEmpty()) {
            return String.format("https://%s/%s", cloudFrontDomain, s3Key);
        } else {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IOException(String.format("File size %d exceeds maximum allowed size %d",
                    file.getSize(), maxFileSize));
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("File name is required");
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("File type not supported. Allowed types: " +
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("File is not a valid image");
            }
        } catch (Exception e) {
            throw new IOException("Invalid image file", e);
        }
    }

    private byte[] processImage(MultipartFile file, PhotoType photoType) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Cannot read image file");
        }

        BufferedImage processedImage = originalImage;
        if (photoType.requiresResizing()) {
            int[] maxDimensions = photoType.getMaxDimensions();
            if (photoType == PhotoType.AVATAR || photoType == PhotoType.THUMBNAIL) {
                processedImage = resizeToSquare(originalImage, maxDimensions[0]);
            } else {
                processedImage = resizeIfNeeded(originalImage, maxDimensions[0], maxDimensions[1]);
            }
        } else {
            processedImage = resizeIfNeeded(originalImage, MAX_WIDTH, MAX_HEIGHT);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String formatName = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!formatName.equals("png") && !formatName.equals("gif")) {
            formatName = "jpg";
        }
        ImageIO.write(processedImage, formatName, outputStream);
        return outputStream.toByteArray();
    }

    private BufferedImage resizeToSquare(BufferedImage originalImage, int size) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int cropSize = Math.min(width, height);
        int x = (width - cropSize) / 2;
        int y = (height - cropSize) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);
        BufferedImage resizedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(croppedImage, 0, 0, size, size, null);
        g2d.dispose();
        return resizedImage;
    }

    private BufferedImage resizeIfNeeded(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return originalImage;
        }

        double aspectRatio = (double) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = maxWidth;
            newHeight = (int) (maxWidth / aspectRatio);
        } else {
            newHeight = maxHeight;
            newWidth = (int) (maxHeight * aspectRatio);
        }

        if (newWidth > maxWidth) {
            newWidth = maxWidth;
            newHeight = (int) (maxWidth / aspectRatio);
        }
        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = (int) (maxHeight * aspectRatio);
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    private Dimension getImageDimensions(byte[] imageData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Cannot read image data");
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex >= 0 ? filename.substring(lastDotIndex + 1) : "";
    }
}