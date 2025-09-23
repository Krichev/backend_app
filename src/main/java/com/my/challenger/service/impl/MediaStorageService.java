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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaStorageService {

    private final MediaFileRepository mediaFileRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", "video/webm");
    private static final Set<String> AUDIO_TYPES = Set.of("audio/mp3", "audio/wav", "audio/ogg", "audio/m4a", "audio/aac");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "application/msword", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv", "flv", "webm");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "m4a", "aac");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");

    /**
     * Store media file with explicit category specification
     */
    @Transactional
    public MediaFile storeMedia(MultipartFile file, Long entityId, MediaCategory category, Long uploadedBy) {
        try {
            validateFile(file);

            // Determine MediaType from content type
            MediaType mediaType = determineMediaType(file.getContentType());

            // Create upload directory
            Path uploadPath = createUploadDirectory(mediaType, category);

            // Generate unique filename
            String uniqueFilename = generateUniqueFilename(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath);

            // Create and save media file entity
            MediaFile mediaFile = createMediaFileEntity(file, filePath, entityId, category, mediaType, uploadedBy);
            mediaFile = mediaFileRepository.save(mediaFile);

            // Generate thumbnail if applicable
            if (mediaType.supportsThumbnails()) {
                generateThumbnailAsync(mediaFile);
            }

            log.info("Media file stored successfully: {} with ID: {}", uniqueFilename, mediaFile.getId());
            return mediaFile;

        } catch (IOException e) {
            log.error("Failed to store media file: {}", file.getOriginalFilename(), e);
            throw new MediaProcessingException("Failed to store media file", e);
        }
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
     * Determine MediaType based on content type
     */
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

    /**
     * Determine MediaCategory based on business context
     * This is a simplified logic - you can enhance based on your business rules
     */
    private MediaCategory determineMediaCategoryFromContext(Long entityId, Long uploadedBy) {
        // Default logic - you can enhance this based on your business requirements
        if (entityId == null) {
            return MediaCategory.TEMPORARY;
        }

        // You can add more sophisticated logic here based on:
        // - Entity type (if entityId represents different types)
        // - User roles
        // - Request context
        // - URL patterns
        // etc.

        return MediaCategory.TEMPORARY; // Default fallback
    }

    /**
     * Get media category based on upload context and business rules
     */
    public MediaCategory getMediaCategoryFromContext(String contextType, Long entityId) {
        if (contextType == null) {
            return MediaCategory.TEMPORARY;
        }

        switch (contextType.toLowerCase()) {
            case "avatar":
            case "profile":
                return MediaCategory.AVATAR;
            case "quiz":
            case "question":
                return MediaCategory.QUIZ_QUESTION;
            case "challenge":
            case "proof":
                return MediaCategory.CHALLENGE_PROOF;
            case "system":
            case "admin":
                return MediaCategory.SYSTEM;
            default:
                return MediaCategory.TEMPORARY;
        }
    }

    private MediaFile createMediaFileEntity(MultipartFile file, Path filePath, Long entityId,
                                            MediaCategory category, MediaType mediaType, Long uploadedBy) throws IOException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFilename(file.getOriginalFilename());
        mediaFile.setContentType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setFilePath(filePath.toString());
        mediaFile.setEntityId(entityId);
        mediaFile.setMediaCategory(category);
        mediaFile.setMediaType(mediaType);
        mediaFile.setUploadedBy(uploadedBy);
        mediaFile.setProcessingStatus(ProcessingStatus.PENDING);

        // Set metadata based on file type
        if (mediaType == MediaType.IMAGE) {
            setImageMetadata(mediaFile, file);
        }

        // Generate S3 key if using cloud storage
        mediaFile.setS3Key(generateS3Key(category, mediaFile.getFilename()));

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

    private String generateS3Key(MediaCategory category, String filename) {
        return category.getS3Prefix() +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/")) +
                filename;
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

    private void generateImageThumbnail(MediaFile mediaFile) throws IOException {
        Path sourcePath = Paths.get(mediaFile.getFilePath());
        Path thumbnailDir = sourcePath.getParent().resolve("thumbnails");
        Files.createDirectories(thumbnailDir);

        String thumbnailFilename = "thumb_" + sourcePath.getFileName().toString();
        Path thumbnailPath = thumbnailDir.resolve(thumbnailFilename);

        BufferedImage originalImage = ImageIO.read(sourcePath.toFile());
        if (originalImage != null) {
            // Create thumbnail (e.g., 150x150)
            int thumbnailSize = 150;
            BufferedImage thumbnailImage = new BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnailImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, thumbnailSize, thumbnailSize, null);
            g2d.dispose();

            ImageIO.write(thumbnailImage, "jpg", thumbnailPath.toFile());
            mediaFile.setThumbnailPath(thumbnailPath.toString());
        }
    }

    private void generateVideoThumbnail(MediaFile mediaFile) {
        try {
            Path sourcePath = Paths.get(mediaFile.getFilePath());
            Path thumbnailDir = sourcePath.getParent().resolve("thumbnails");
            Files.createDirectories(thumbnailDir);

            String thumbnailFilename = sourcePath.getFileName().toString().replaceAll("\\.[^.]+$", ".jpg");
            Path thumbnailPath = thumbnailDir.resolve(thumbnailFilename);

            // Video thumbnail generation logic would go here using FFmpeg
            // For now, create a placeholder
            mediaFile.setThumbnailPath(thumbnailPath.toString());
            mediaFileRepository.save(mediaFile);

        } catch (IOException e) {
            log.error("Failed to generate video thumbnail for: {}", mediaFile.getId(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (!IMAGE_TYPES.contains(contentType) &&
                !VIDEO_TYPES.contains(contentType) &&
                !AUDIO_TYPES.contains(contentType) &&
                !DOCUMENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        if (!isValidExtension(extension, contentType)) {
            throw new IllegalArgumentException("Invalid file extension: " + extension);
        }
    }

    private boolean isValidExtension(String extension, String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return IMAGE_EXTENSIONS.contains(extension);
        } else if (VIDEO_TYPES.contains(contentType)) {
            return VIDEO_EXTENSIONS.contains(extension);
        } else if (AUDIO_TYPES.contains(contentType)) {
            return AUDIO_EXTENSIONS.contains(extension);
        } else if (DOCUMENT_TYPES.contains(contentType)) {
            return DOCUMENT_EXTENSIONS.contains(extension);
        }
        return false;
    }

    private Path createUploadDirectory(MediaType mediaType, MediaCategory category) throws IOException {
        String typeDir = mediaType.name().toLowerCase();
        String categoryDir = category.name().toLowerCase().replace('_', '-');
        Path uploadPath = Paths.get(uploadDir, typeDir, categoryDir);
        Files.createDirectories(uploadPath);
        return uploadPath;
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);
        return String.format("%s_%s.%s", timestamp, randomId, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // Additional utility methods for retrieving media

    public List<MediaFile> getMediaByCategory(MediaCategory category) {
        return mediaFileRepository.findByMediaCategory(category);
    }

    public List<MediaFile> getMediaByTypeAndUser(MediaType mediaType, Long uploadedBy) {
        return mediaFileRepository.findByMediaTypeAndUploadedBy(mediaType, uploadedBy);
    }

    public List<MediaFile> getUserAvatars(Long userId) {
        return mediaFileRepository.findByUploadedByAndMediaCategory(userId, MediaCategory.AVATAR);
    }

    public List<MediaFile> getChallengeProofs(Long challengeId) {
        return mediaFileRepository.findByEntityIdAndMediaCategory(challengeId, MediaCategory.CHALLENGE_PROOF);
    }

    public List<MediaFile> getQuizQuestionMedia(Long questionId) {
        return mediaFileRepository.findByEntityIdAndMediaCategory(questionId, MediaCategory.QUIZ_QUESTION);
    }

    public Optional<MediaFile> findById(Long id) {
        return mediaFileRepository.findById(id);
    }

    public Optional<MediaFile> findByFilename(String filename) {
        return mediaFileRepository.findByFilename(filename);
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        Optional<MediaFile> mediaFile = mediaFileRepository.findById(mediaId);
        if (mediaFile.isPresent()) {
            try {
                // Delete physical file
                Path filePath = Paths.get(mediaFile.get().getFilePath());
                Files.deleteIfExists(filePath);

                // Delete thumbnail if exists
                if (mediaFile.get().getThumbnailPath() != null) {
                    Path thumbnailPath = Paths.get(mediaFile.get().getThumbnailPath());
                    Files.deleteIfExists(thumbnailPath);
                }

                // Delete database record
                mediaFileRepository.deleteById(mediaId);

                log.info("Media file deleted successfully: {}", mediaId);
            } catch (IOException e) {
                log.error("Failed to delete media file: {}", mediaId, e);
                throw new MediaProcessingException("Failed to delete media file", e);
            }
        }
    }

    /**
     * Promote temporary media to a specific category
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
        log.info("Cleaned up {} temporary files older than {} days", tempFiles.size(), olderThanDays);
    }
}