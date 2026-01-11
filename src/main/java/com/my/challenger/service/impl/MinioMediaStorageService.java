package com.my.challenger.service.impl;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO-based media storage service using S3-compatible API
 * Replaces file-based storage with object storage
 */
@Service
//@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
@RequiredArgsConstructor
@Slf4j
public class MinioMediaStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaFileRepository mediaFileRepository;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.storage.s3.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${app.storage.minio.presigned-url-duration:60}")
    private int presignedUrlDuration; // minutes

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm");
    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/wav", "audio/mp3", "audio/ogg", "audio/m4a",
            "audio/aac", "audio/mpeg", "audio/x-wav", "audio/webm"
    );
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "application/msword", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;

    /**
     * Store media file with explicit category specification
     */
    @Transactional
    public MediaFile storeMedia(MultipartFile file, Long entityId, MediaCategory category, Long uploadedBy) {
        try {
            validateFile(file);

            // Determine MediaType from content type
            MediaType mediaType = determineMediaType(file.getContentType());

            // Generate unique S3 key
            String s3Key = generateS3Key(category, file.getOriginalFilename());

            // Upload to MinIO
            uploadToMinio(s3Key, file.getBytes(), file.getContentType());

            // Create and save media file entity
            MediaFile mediaFile = createMediaFileEntity(file, s3Key, entityId, category, mediaType, uploadedBy);
            mediaFile = mediaFileRepository.save(mediaFile);

            // Generate thumbnail if applicable
            if (mediaType.supportsThumbnails()) {
                generateThumbnailAsync(mediaFile);
            }

            log.info("Media file stored successfully in MinIO: {} with ID: {}", s3Key, mediaFile.getId());
            return mediaFile;

        } catch (IOException e) {
            log.error("Failed to store media file: {}", file.getOriginalFilename(), e);
            throw new MediaProcessingException("Failed to store media file", e);
        }
    }

    /**
     * Update media entity with the question ID after question creation
     */
    public void updateMediaEntityId(Long mediaFileId, Long questionId) {
        try {
            MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                    .orElseThrow(() -> new IllegalArgumentException("Media file not found"));

            mediaFile.setEntityId(questionId);
            mediaFileRepository.save(mediaFile);

            log.debug("Updated media file {} with question ID {}", mediaFileId, questionId);
        } catch (Exception e) {
            log.warn("Failed to update media entity ID, but question creation succeeded", e);
            // Don't fail the entire transaction if this update fails
        }
    }

    /**
     * Upload file to MinIO
     */
    private void uploadToMinio(String s3Key, byte[] content, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.debug("Uploaded file to MinIO: {}", s3Key);

        } catch (Exception e) {
            log.error("Failed to upload to MinIO: {}", s3Key, e);
            throw new MediaProcessingException("Failed to upload to MinIO", e);
        }
    }

    /**
     * Get media file by ID
     */
    public MediaFile getMediaFileById(Long mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media file not found with ID: " + mediaId));
    }

    /**
     * Download file from MinIO
     */
    public byte[] downloadFromMinio(String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            InputStream inputStream = s3Client.getObject(getRequest);
            return inputStream.readAllBytes();

        } catch (Exception e) {
            log.error("Failed to download from MinIO: {}", s3Key, e);
            throw new MediaProcessingException("Failed to download from MinIO", e);
        }
    }

    /**
     * Delete media file with authorization check
     */
    @Transactional
    public void deleteMedia(Long mediaId, Long userId) {
        MediaFile mediaFile = getMediaFileById(mediaId);

        // Check if user is authorized to delete
        if (!mediaFile.getUploadedBy().equals(userId)) {
            throw new IllegalStateException("You are not authorized to delete this media file");
        }

        // Delete from MinIO
        try {
            deleteFromMinio(mediaFile.getS3Key());

            if (mediaFile.getThumbnailPath() != null) {
                deleteFromMinio(mediaFile.getThumbnailPath());
            }
        } catch (Exception e) {
            log.error("Error deleting files from MinIO for media {}", mediaId, e);
            // Continue with database deletion even if MinIO deletion fails
        }

        // Delete database record
        mediaFileRepository.delete(mediaFile);
        log.info("Media file {} deleted by user {}", mediaId, userId);
    }

    /**
     * Delete file from MinIO
     */
    private void deleteFromMinio(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Deleted file from MinIO: {}", s3Key);

        } catch (Exception e) {
            log.error("Failed to delete from MinIO: {}", s3Key, e);
            throw new MediaProcessingException("Failed to delete from MinIO", e);
        }
    }

    /**
     * Delete media file without authorization check (for system operations)
     */
    @Transactional
    public void deleteMedia(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);

        try {
            deleteFromMinio(mediaFile.getS3Key());

            if (mediaFile.getThumbnailPath() != null) {
                deleteFromMinio(mediaFile.getThumbnailPath());
            }
        } catch (Exception e) {
            log.error("Error deleting files from MinIO for media {}", mediaId, e);
        }

        mediaFileRepository.delete(mediaFile);
        log.info("Media file {} deleted by system", mediaId);
    }

    /**
     * Get public URL for media file (using presigned URL)
     */
    public String getMediaUrl(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }
        return generatePresignedUrl(mediaFile.getS3Key());
    }

    /**
     * Get public URL for thumbnail
     */
    public String getThumbnailUrl(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getThumbnailPath() == null) {
            return null;
        }
        return generatePresignedUrl(mediaFile.getThumbnailPath());
    }

    /**
     * Get media URL by media ID
     */
    public String getMediaUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }

        Optional<MediaFile> mediaFile = mediaFileRepository.findById(mediaId);
        if (mediaFile.isPresent()) {
            return generatePresignedUrl(mediaFile.get().getS3Key());
        }

        log.warn("Media file not found for ID: {}", mediaId);
        return null;
    }

    /**
     * Get media URL by filename
     */
    public String getMediaUrl(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }

        Optional<MediaFile> mediaFile = mediaFileRepository.findByFilename(filename);
        if (mediaFile.isPresent()) {
            return generatePresignedUrl(mediaFile.get().getS3Key());
        }

        log.warn("Media file not found for filename: {}", filename);
        return null;
    }

    /**
     * Generate presigned URL for temporary access
     */
    private String generatePresignedUrl(String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlDuration))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", s3Key, e);
            return null;
        }
    }

    /**
     * Generate presigned URL from S3 key
     * This is used when the entity stores only the S3 key (not full URL)
     */
    public String generatePresignedUrlFromKey(String s3Key) {
        if (s3Key == null || s3Key.trim().isEmpty()) {
            return null;
        }

        // If it's already a full URL (legacy data), extract key first
        if (s3Key.startsWith("http://") || s3Key.startsWith("https://")) {
            log.warn("S3 key appears to be a full URL, extracting key: {}", s3Key);
            s3Key = extractS3KeyFromUrl(s3Key);
            if (s3Key == null) {
                return null;
            }
        }

        return generatePresignedUrl(s3Key);
    }

    /**
     * Extract S3 key from a full MinIO URL (for legacy data migration)
     */
    private String extractS3KeyFromUrl(String url) {
        try {
            // URL format: http://host:port/bucket/key?params
            // We need to extract: key
            java.net.URL parsedUrl = new java.net.URL(url);
            String path = parsedUrl.getPath(); // /bucket/key

            // Remove leading slash and bucket name
            if (path.startsWith("/" + bucketName + "/")) {
                return path.substring(bucketName.length() + 2); // +2 for both slashes
            }

            // Fallback: just remove leading slash
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.error("Failed to extract S3 key from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Get media file by ID
     */
    public Optional<MediaFile> getMediaFile(Long mediaId) {
        return mediaFileRepository.findById(mediaId);
    }

    /**
     * Get media file by filename
     */
    public Optional<MediaFile> getMediaFile(String filename) {
        return mediaFileRepository.findByFilename(filename);
    }

    /**
     * Store media with automatic category determination based on context
     */
    @Transactional
    public MediaFile storeMedia(MultipartFile file, Long entityId, Long uploadedBy) {
        MediaCategory category = determineMediaCategoryFromContext(entityId, uploadedBy);
        return storeMedia(file, entityId, category, uploadedBy);
    }

    /**
     * Store temporary media (e.g., during upload process)
     */
    @Transactional
    public MediaFile storeTemporaryMedia(MultipartFile file, Long uploadedBy) {
        return storeMedia(file, null, MediaCategory.TEMPORARY, uploadedBy);
    }

    /**
     * Store avatar media
     */
    @Transactional
    public MediaFile storeAvatarMedia(MultipartFile file, Long userId) {
        return storeMedia(file, userId, MediaCategory.AVATAR, userId);
    }

    /**
     * Store challenge proof media
     */
    @Transactional
    public MediaFile storeChallengeProofMedia(MultipartFile file, Long challengeId, Long uploadedBy) {
        return storeMedia(file, challengeId, MediaCategory.CHALLENGE_PROOF, uploadedBy);
    }

    /**
     * Store quiz question media
     */
    @Transactional
    public MediaFile storeQuizQuestionMedia(MultipartFile file, Long questionId, Long uploadedBy) {
        return storeMedia(file, questionId, MediaCategory.QUIZ_QUESTION, uploadedBy);
    }

    /**
     * Store system media
     */
    @Transactional
    public MediaFile storeSystemMedia(MultipartFile file, Long uploadedBy) {
        return storeMedia(file, null, MediaCategory.SYSTEM, uploadedBy);
    }

    /**
     * Promote temporary media to permanent category
     */
    @Transactional
    public MediaFile promoteTemporaryMedia(Long mediaId, Long entityId, MediaCategory newCategory) {
        Optional<MediaFile> mediaFileOpt = mediaFileRepository.findById(mediaId);
        if (mediaFileOpt.isPresent()) {
            MediaFile mediaFile = mediaFileOpt.get();
            if (mediaFile.getMediaCategory() == MediaCategory.TEMPORARY) {
                mediaFile.setMediaCategory(newCategory);
                mediaFile.setEntityId(entityId);
                return mediaFileRepository.save(mediaFile);
            }
            throw new IllegalArgumentException("Media is not in temporary status");
        }
        throw new IllegalArgumentException("Media file not found");
    }

    /**
     * Clean up old temporary files
     */
    @Transactional
    public void cleanupTemporaryFiles(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        List<MediaFile> tempFiles = mediaFileRepository.findByMediaCategory(MediaCategory.TEMPORARY)
                .stream()
                .filter(file -> file.getUploadedAt().isBefore(cutoffDate))
                .toList();

        tempFiles.forEach(file -> deleteMedia(file.getId()));
        log.info("Cleaned up {} temporary files older than {} days from MinIO", tempFiles.size(), olderThanDays);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File must have a valid filename");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File must have a valid content type");
        }

        // Validate content type
        if (!isSupportedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private boolean isSupportedContentType(String contentType) {
        return IMAGE_TYPES.contains(contentType) ||
                VIDEO_TYPES.contains(contentType) ||
                AUDIO_TYPES.contains(contentType) ||
                DOCUMENT_TYPES.contains(contentType);
    }

    private String generateS3Key(MediaCategory category, String originalFilename) {
        String filename = generateUniqueFilename(originalFilename);
        return category.getS3Prefix() +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/")) +
                filename;
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private MediaFile createMediaFileEntity(MultipartFile file, String s3Key, Long entityId,
                                            MediaCategory category, MediaType mediaType, Long uploadedBy) throws IOException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFilename(file.getOriginalFilename());
        mediaFile.setOriginalFilename(file.getOriginalFilename());
        mediaFile.setS3Key(s3Key);
        mediaFile.setFilePath(s3Key);
        mediaFile.setFileSize(file.getSize());
        mediaFile.setContentType(file.getContentType());
        mediaFile.setMediaType(mediaType);
        mediaFile.setMediaCategory(category);
        mediaFile.setEntityId(entityId);
        mediaFile.setUploadedBy(uploadedBy);
        mediaFile.setUploadedAt(LocalDateTime.now());
        mediaFile.setProcessingStatus(ProcessingStatus.PENDING);

        // Set image-specific metadata
        if (mediaType == MediaType.IMAGE) {
            setImageMetadata(mediaFile, file);
        }

        return mediaFile;
    }

    private void setImageMetadata(MediaFile mediaFile, MultipartFile file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image != null) {
                mediaFile.setWidth(image.getWidth());
                mediaFile.setHeight(image.getHeight());
                mediaFile.setResolution(image.getWidth() + "x" + image.getHeight());
            }
        } catch (IOException e) {
            log.warn("Could not read image metadata for file: {}", file.getOriginalFilename());
        }
    }

    @Async
    protected void generateThumbnailAsync(MediaFile mediaFile) {
        try {
            if (mediaFile.getMediaType() == MediaType.IMAGE) {
                generateImageThumbnail(mediaFile);
            } else if (mediaFile.getMediaType() == MediaType.VIDEO) {
                generateVideoThumbnail(mediaFile);
            }
            mediaFile.setProcessingStatus(ProcessingStatus.COMPLETED);
            mediaFileRepository.save(mediaFile);
        } catch (Exception e) {
            log.error("Failed to generate thumbnail for media file: {}", mediaFile.getId(), e);
            mediaFile.setProcessingStatus(ProcessingStatus.FAILED);
            mediaFileRepository.save(mediaFile);
        }
    }

    private void generateImageThumbnail(MediaFile mediaFile) {
        try {
            // Download original image from MinIO
            byte[] originalImage = downloadFromMinio(mediaFile.getS3Key());

            // Create thumbnail
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImage));
            if (image == null) {
                log.warn("Could not read image for thumbnail generation: {}", mediaFile.getFilename());
                return;
            }

            BufferedImage thumbnail = createThumbnail(image);

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] thumbnailBytes = baos.toByteArray();

            // Generate thumbnail S3 key
            String thumbnailKey = generateThumbnailKey(mediaFile.getS3Key());

            // Upload thumbnail to MinIO
            uploadToMinio(thumbnailKey, thumbnailBytes, "image/jpeg");

            // Update media file record
            mediaFile.setThumbnailPath(thumbnailKey);
            mediaFileRepository.save(mediaFile);

            log.info("Thumbnail generated for image: {}", mediaFile.getFilename());

        } catch (Exception e) {
            log.error("Failed to generate image thumbnail: {}", mediaFile.getFilename(), e);
        }
    }

    private void generateVideoThumbnail(MediaFile mediaFile) {
        // Video thumbnail generation would require FFmpeg or similar
        // For now, we'll skip video thumbnail generation
        log.info("Video thumbnail generation not yet implemented for: {}", mediaFile.getFilename());
    }

    private BufferedImage createThumbnail(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Calculate new dimensions maintaining aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = THUMBNAIL_WIDTH;
        int newHeight = (int) (newWidth / aspectRatio);

        if (newHeight > THUMBNAIL_HEIGHT) {
            newHeight = THUMBNAIL_HEIGHT;
            newWidth = (int) (newHeight * aspectRatio);
        }

        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return thumbnail;
    }

    private String generateThumbnailKey(String originalKey) {
        int lastSlashIndex = originalKey.lastIndexOf('/');
        String path = originalKey.substring(0, lastSlashIndex + 1);
        String filename = originalKey.substring(lastSlashIndex + 1);
        return path + "thumb_" + filename;
    }

    private MediaType determineMediaType(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return MediaType.IMAGE;
        } else if (VIDEO_TYPES.contains(contentType)) {
            return MediaType.VIDEO;
        } else if (AUDIO_TYPES.contains(contentType)) {
            return MediaType.AUDIO;
        } else if (DOCUMENT_TYPES.contains(contentType)) {
            return MediaType.DOCUMENT;
        } else {
            throw new IllegalArgumentException("Unsupported media type: " + contentType);
        }
    }

    private MediaCategory determineMediaCategoryFromContext(Long entityId, Long uploadedBy) {
        if (entityId == null) {
            return MediaCategory.TEMPORARY;
        }
        return MediaCategory.QUIZ_QUESTION;
    }
}