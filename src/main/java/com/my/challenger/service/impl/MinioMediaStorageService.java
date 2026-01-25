package com.my.challenger.service.impl;

import com.my.challenger.config.StorageProperties;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.service.BucketResolver;
import com.my.challenger.util.S3KeyGenerator;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO-based media storage service using S3-compatible API
 * Replaces file-based storage with object storage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioMediaStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaFileRepository mediaFileRepository;
    
    // NEW DEPENDENCIES
    private final S3KeyGenerator s3KeyGenerator;
    private final BucketResolver bucketResolver;
    private final StorageProperties storageProperties;

    // Legacy bucket name for backward compatibility
    @Value("${app.storage.s3.bucket-name}")
    private String legacyBucketName;

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
            "audio/aac", "audio/mpeg", "audio/x-wav", "audio/webm",
            "audio/x-m4a", "audio/mp4", "audio/3gpp", "audio/amr"
    );
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "application/msword", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;

    /**
     * Store media file with explicit category specification - REFACTORED
     */
    @Transactional
    public MediaFile storeMedia(MultipartFile file, Long entityId, MediaCategory category, Long uploadedBy) {
        // Delegate to new method signature with explicit quiz/question IDs (null for general use)
        return storeMedia(file, null, null, category, uploadedBy);
    }

    /**
     * Store media file with full context - NEW METHOD
     */
    @Transactional
    public MediaFile storeMedia(MultipartFile file, Long quizId, Long questionId, 
                                MediaCategory category, Long uploadedBy) {
        try {
            // Log incoming file details
            log.info("📦 storeMedia called:");
            log.info("   - Filename: {}", file.getOriginalFilename());
            log.info("   - Content-Type: {}", file.getContentType());
            log.info("   - Size: {} bytes", file.getSize());
            log.info("   - Category: {}", category);

            validateFile(file);
            
            byte[] fileContent = file.getBytes();
            
            // Calculate content hash for deduplication and integrity
            String contentHash = calculateContentHash(fileContent);
            
            // Optional: Check for duplicate content from same user
            if (contentHash != null) {
                Optional<MediaFile> existing = mediaFileRepository
                    .findByContentHashAndUploadedBy(contentHash, uploadedBy)
                    .stream().findFirst();
                if (existing.isPresent()) {
                    log.info("Duplicate file detected for user {}, returning existing media {}", 
                        uploadedBy, existing.get().getId());
                    return existing.get();
                }
            }

            // Determine MediaType from content type with fallback
            String contentType = file.getContentType();
            MediaType mediaType = determineMediaTypeWithFallback(contentType, file.getOriginalFilename());
            log.info("   - Determined MediaType: {}", mediaType);

            // Resolve bucket
            String bucket = bucketResolver.getBucket(mediaType);
            log.info("   - Resolved Bucket: {}", bucket);

            // Generate unique S3 key using new hierarchical schema
            String s3Key = s3KeyGenerator.generateKey(
                storageProperties.getEnvironment(),
                uploadedBy,
                "user", // Default owner type
                quizId,
                questionId,
                mediaType,
                getFileExtension(file.getOriginalFilename())
            );
            log.info("   - Generated S3 Key: {}", s3Key);

            // Upload to MinIO
            uploadToMinio(bucket, s3Key, fileContent, contentType);
            log.info("   - Upload to MinIO: SUCCESS");

            // Create and save media file entity
            // Use entityId (questionId) if available, otherwise 0 or appropriate mapping
            Long entityRefId = (questionId != null) ? questionId : (quizId != null ? quizId : 0L);
            
            MediaFile mediaFile = createMediaFileEntity(file, bucket, s3Key, entityRefId, category, mediaType, uploadedBy, contentHash);
            mediaFile = mediaFileRepository.save(mediaFile);
            log.info("   - MediaFile saved with ID: {}", mediaFile.getId());

            // Generate thumbnail if applicable
            if (mediaType.supportsThumbnails()) {
                generateThumbnailAsync(mediaFile);
            }

            log.info("Media file stored successfully in bucket {}: {} with ID: {}", bucket, s3Key, mediaFile.getId());
            return mediaFile;

        } catch (IOException e) {
            log.error("❌ Failed to store media file: {}", file.getOriginalFilename(), e);
            throw new MediaProcessingException("Failed to store media file", e);
        }
    }

    private MediaType determineMediaTypeWithFallback(String contentType, String filename) {
        // First try by content type
        if (contentType != null) {
            if (IMAGE_TYPES.contains(contentType)) return MediaType.IMAGE;
            if (VIDEO_TYPES.contains(contentType)) return MediaType.VIDEO;
            if (AUDIO_TYPES.contains(contentType)) return MediaType.AUDIO;
            if (DOCUMENT_TYPES.contains(contentType)) return MediaType.DOCUMENT;
            if (contentType.startsWith("audio/")) return MediaType.AUDIO;
            if (contentType.startsWith("video/")) return MediaType.VIDEO;
            if (contentType.startsWith("image/")) return MediaType.IMAGE;
        }
        
        // Fallback to filename extension
        if (filename != null) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "mp3", "wav", "m4a", "aac", "ogg", "webm", "amr", "3gp" -> MediaType.AUDIO;
                case "mp4", "mov", "avi", "mkv" -> MediaType.VIDEO;
                case "jpg", "jpeg", "png", "gif", "webp" -> MediaType.IMAGE;
                case "pdf", "doc", "docx" -> MediaType.DOCUMENT;
                default -> throw new IllegalArgumentException("Cannot determine media type for: " + filename);
            };
        }
        
        throw new IllegalArgumentException("Cannot determine media type: contentType=" + contentType + ", filename=" + filename);
    }

    public boolean fileExists(String s3Key) {
        // This is tricky without knowing the bucket.
        // We'll try all buckets or assume legacy if we don't have the media record.
        // Ideally, this method should take a MediaFile or bucket name.
        // For backward compatibility, we check legacy bucket first.
        if (fileExistsInBucket(legacyBucketName, s3Key)) return true;
        
        // Check other buckets if configured
        if (storageProperties.getS3().getBuckets() != null) {
             if (fileExistsInBucket(storageProperties.getS3().getBuckets().getImages(), s3Key)) return true;
             if (fileExistsInBucket(storageProperties.getS3().getBuckets().getAudio(), s3Key)) return true;
             if (fileExistsInBucket(storageProperties.getS3().getBuckets().getVideos(), s3Key)) return true;
        }
        
        return false;
    }
    
    private boolean fileExistsInBucket(String bucket, String key) {
        if (bucket == null) return false;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            // log.debug("Error checking file existence in {}: {}", bucket, e.getMessage());
            return false;
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
    private void uploadToMinio(String bucket, String s3Key, byte[] content, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.debug("Uploaded file to MinIO bucket {}: {}", bucket, s3Key);

        } catch (Exception e) {
            log.error("Failed to upload to MinIO: {}/{}", bucket, s3Key, e);
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
        // We need to find the media file to know the bucket
        // If we only have the key, we have to guess or search. 
        // NOTE: This method signature assumes we don't have the MediaFile object.
        // IMPORTANT: We should update callers to use downloadFromMinio(MediaFile) instead.
        
        // Strategy: Try legacy bucket first, then guess based on key path (if hierarchical)
        // or search all buckets.
        
        // For now, let's try to look up the MediaFile by S3 Key
        Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByS3Key(s3Key);
        String bucket = legacyBucketName;
        
        if (mediaFileOpt.isPresent() && mediaFileOpt.get().getBucketName() != null) {
            bucket = mediaFileOpt.get().getBucketName();
        }
        
        return downloadFromMinio(bucket, s3Key);
    }
    
    public byte[] downloadFromMinio(MediaFile mediaFile) {
        String bucket = mediaFile.getBucketName() != null ? mediaFile.getBucketName() : legacyBucketName;
        return downloadFromMinio(bucket, mediaFile.getS3Key());
    }

    public byte[] downloadFromMinio(String bucket, String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            InputStream inputStream = s3Client.getObject(getRequest);
            return inputStream.readAllBytes();

        } catch (Exception e) {
            log.error("Failed to download from MinIO: {}/{}", bucket, s3Key, e);
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

        performDelete(mediaFile);
        log.info("Media file {} deleted by user {}", mediaId, userId);
    }
    
    /**
     * Delete media file without authorization check (for system operations)
     */
    @Transactional
    public void deleteMedia(Long mediaId) {
        MediaFile mediaFile = getMediaFileById(mediaId);
        performDelete(mediaFile);
        log.info("Media file {} deleted by system", mediaId);
    }

    private void performDelete(MediaFile mediaFile) {
        String bucket = mediaFile.getBucketName() != null ? mediaFile.getBucketName() : legacyBucketName;
        
        try {
            deleteFromMinio(bucket, mediaFile.getS3Key());

            if (mediaFile.getThumbnailPath() != null) {
                deleteFromMinio(bucket, mediaFile.getThumbnailPath());
            }
        } catch (Exception e) {
            log.error("Error deleting files from MinIO for media {}", mediaFile.getId(), e);
            // Continue with database deletion even if MinIO deletion fails
        }

        mediaFileRepository.delete(mediaFile);
    }

    /**
     * Delete file from MinIO
     */
    private void deleteFromMinio(String bucket, String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Deleted file from MinIO: {}/{}", bucket, s3Key);

        } catch (Exception e) {
            log.error("Failed to delete from MinIO: {}/{}", bucket, s3Key, e);
            throw new MediaProcessingException("Failed to delete from MinIO", e);
        }
    }

    /**
     * Get public URL for media file (using presigned URL)
     */
    public String getMediaUrl(MediaFile mediaFile) {
        if (mediaFile == null) {
            return null;
        }
        String bucket = mediaFile.getBucketName() != null ? mediaFile.getBucketName() : legacyBucketName;
        return generatePresignedUrl(bucket, mediaFile.getS3Key());
    }

    /**
     * Get public URL for thumbnail
     */
    public String getThumbnailUrl(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getThumbnailPath() == null) {
            return null;
        }
        String bucket = mediaFile.getBucketName() != null ? mediaFile.getBucketName() : legacyBucketName;
        return generatePresignedUrl(bucket, mediaFile.getThumbnailPath());
    }

    /**
     * Get media file by storage key
     */
    public Optional<MediaFile> getMediaFileByStorageKey(UUID storageKey) {
        return mediaFileRepository.findByStorageKey(storageKey);
    }

    /**
     * Get public URL for media file by storage key
     */
    public String getMediaUrlByStorageKey(UUID storageKey) {
        return mediaFileRepository.findByStorageKey(storageKey)
            .map(mf -> {
                String bucket = mf.getBucketName() != null ? mf.getBucketName() : legacyBucketName;
                return generatePresignedUrl(bucket, mf.getS3Key());
            })
            .orElse(null);
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
            return getMediaUrl(mediaFile.get());
        }

        log.warn("Media file not found for ID: {}", mediaId);
        return null;
    }

    /**
     * Get media URL by filename
     * @deprecated use getMediaUrlByStorageKey(UUID) instead
     */
    @Deprecated
    public String getMediaUrl(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return null;
        }
        
        log.warn("getMediaUrl(filename) is deprecated, use getMediaUrlByStorageKey(UUID)");

        Optional<MediaFile> mediaFile = mediaFileRepository.findByFilename(filename);
        if (mediaFile.isPresent()) {
            return getMediaUrl(mediaFile.get());
        }

        log.warn("Media file not found for filename: {}", filename);
        return null;
    }

    /**
     * Generate presigned URL for temporary access
     */
    public String generatePresignedUrl(String bucket, String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlDuration))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}/{}", bucket, s3Key, e);
            return null;
        }
    }

    /**
     * Generate presigned URL from S3 key
     * This is used when the entity stores only the S3 key (not full URL)
     * Assumes legacy bucket if we don't have context.
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

        // We assume legacy bucket here since we don't have the media object to tell us otherwise.
        // If the key structure is new (has bucket info implied?), we still need the bucket name.
        // Ideally, callers should use the method taking MediaFile.
        return generatePresignedUrl(legacyBucketName, s3Key);
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
            // For this to work robustly with multiple buckets, we'd need to check all known bucket names.
            // For now, checking legacy bucket is a safe default for legacy URLs.
            if (path.startsWith("/" + legacyBucketName + "/")) {
                return path.substring(legacyBucketName.length() + 2); // +2 for both slashes
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
        // We pass entityId as quizId/questionId depending on context, or null if ambiguous
        // For general usage, we just use the simple storeMedia which maps to defaults
        return storeMedia(file, entityId, category, uploadedBy);
    }

    /**
     * Store temporary media (e.g., during upload process)
     */
    @Transactional
    public MediaFile storeTemporaryMedia(MultipartFile file, Long uploadedBy) {
        return storeMedia(file, null, null, MediaCategory.TEMPORARY, uploadedBy);
    }

    /**
     * Store avatar media
     */
    @Transactional
    public MediaFile storeAvatarMedia(MultipartFile file, Long userId) {
        return storeMedia(file, null, null, MediaCategory.AVATAR, userId);
    }

    /**
     * Store challenge proof media
     */
    @Transactional
    public MediaFile storeChallengeProofMedia(MultipartFile file, Long challengeId, Long uploadedBy) {
        // Mapping challengeId as quizId context for now, or just generic entity
        return storeMedia(file, challengeId, null, MediaCategory.CHALLENGE_PROOF, uploadedBy);
    }

    /**
     * Store quiz question media
     */
    @Transactional
    public MediaFile storeQuizQuestionMedia(MultipartFile file, Long questionId, Long uploadedBy) {
        return storeMedia(file, null, questionId, MediaCategory.QUIZ_QUESTION, uploadedBy);
    }

    /**
     * Store system media
     */
    @Transactional
    public MediaFile storeSystemMedia(MultipartFile file, Long uploadedBy) {
        return storeMedia(file, null, null, MediaCategory.SYSTEM, uploadedBy);
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

    private String calculateContentHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, skipping content hash");
            return null;
        }
    }

    private String extractFilenameFromS3Key(String s3Key) {
        int lastSlash = s3Key.lastIndexOf('/');
        return lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
    }

    private MediaFile createMediaFileEntity(MultipartFile file, String bucketName, String s3Key, Long entityId,
                                            MediaCategory category, MediaType mediaType, 
                                            Long uploadedBy, String contentHash) throws IOException {
        MediaFile mediaFile = new MediaFile();
        
        // Store UUID-based filename (extracted from s3Key), not original
        String storageFilename = extractFilenameFromS3Key(s3Key);
        mediaFile.setFilename(storageFilename);
        
        // Preserve original filename for display
        mediaFile.setOriginalFilename(file.getOriginalFilename());
        
        mediaFile.setBucketName(bucketName);
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
        
        // Set content hash for deduplication
        mediaFile.setContentHash(contentHash);

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
            byte[] originalImage = downloadFromMinio(mediaFile);

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

            // Generate thumbnail S3 key (keep in same bucket and path, just prefixed)
            // For hierarchical keys, we might want: .../image/thumb_uuid.jpg
            String thumbnailKey = generateThumbnailKey(mediaFile.getS3Key());
            String bucket = mediaFile.getBucketName() != null ? mediaFile.getBucketName() : legacyBucketName;

            // Upload thumbnail to MinIO
            uploadToMinio(bucket, thumbnailKey, thumbnailBytes, "image/jpeg");

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
