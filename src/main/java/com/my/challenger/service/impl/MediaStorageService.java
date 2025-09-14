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

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv", "flv", "webm");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "m4a", "aac");

    private static final int AVATAR_SIZE = 400;
    private static final int MAX_IMAGE_WIDTH = 2048;
    private static final int MAX_IMAGE_HEIGHT = 2048;

    @Transactional
    public MediaFile saveMedia(MultipartFile file, MediaType mediaType, Long entityId, Long uploadedBy) throws IOException {
        validateFile(file);

        MediaCategory category = determineMediaCategory(file.getContentType());
        Path uploadPath = createUploadDirectory(mediaType, category);
        String uniqueFilename = generateUniqueFilename(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(uniqueFilename);

        // Save file to disk
        Files.write(filePath, file.getBytes());

        // Create media file entity using builder pattern
        MediaFile mediaFile = MediaFile.builder()
                .originalFilename(file.getOriginalFilename())
                .filename(uniqueFilename)
                .filePath(filePath.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .mediaType(mediaType)
                .mediaCategory(category)
                .entityId(entityId)
                .uploadedBy(uploadedBy)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        // Save to database (uploadedAt will be set by @CreationTimestamp)
        mediaFile = mediaFileRepository.save(mediaFile);

        // Process asynchronously
        processMediaAsync(mediaFile);

        return mediaFile;
    }

    // **MISSING METHOD 1: getMedia**
    @Transactional(readOnly = true)
    public Optional<MediaFile> getMedia(Long mediaId) {
        return mediaFileRepository.findById(mediaId);
    }

    // **MISSING METHOD 2: getMediaByEntityAndType**
    @Transactional(readOnly = true)
    public Optional<MediaFile> getMediaByEntityAndType(Long entityId, MediaType mediaType) {
        List<MediaFile> mediaFiles = mediaFileRepository.findByEntityIdAndMediaType(entityId, mediaType);
        return mediaFiles.isEmpty() ? Optional.empty() : Optional.of(mediaFiles.get(0));
    }

    // **MISSING METHOD 3: getMediaData**
    public byte[] getMediaData(MediaFile mediaFile) throws IOException {
        if (mediaFile == null) {
            throw new IllegalArgumentException("MediaFile cannot be null");
        }

        Path filePath = mediaFile.getProcessedPath() != null
                ? Paths.get(mediaFile.getProcessedPath())
                : Paths.get(mediaFile.getFilePath());

        if (!Files.exists(filePath)) {
            throw new IOException("Media file not found: " + filePath.toString());
        }

        return Files.readAllBytes(filePath);
    }

    // **MISSING METHOD 4: getThumbnailData**
    public byte[] getThumbnailData(MediaFile mediaFile) throws IOException {
        if (mediaFile == null || mediaFile.getThumbnailPath() == null) {
            return null;
        }

        Path thumbnailPath = Paths.get(mediaFile.getThumbnailPath());
        if (!Files.exists(thumbnailPath)) {
            return null;
        }

        return Files.readAllBytes(thumbnailPath);
    }

    // **MISSING METHOD 5: getThumbnailUrl**
    public String getThumbnailUrl(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getId() == null) {
            return null;
        }

        // Check if thumbnail exists
        if (mediaFile.getThumbnailPath() != null && Files.exists(Paths.get(mediaFile.getThumbnailPath()))) {
            return baseUrl + "/api/media/" + mediaFile.getId() + "/thumbnail";
        }

        // For images, return the original media URL as thumbnail
        if (mediaFile.getMediaCategory() == MediaCategory.IMAGE) {
            return getMediaUrl(mediaFile);
        }

        return null;
    }

    // **MISSING METHOD 6: getMediaByTypeAndUser**
    @Transactional(readOnly = true)
    public List<MediaFile> getMediaByTypeAndUser(MediaType mediaType, Long userId) {
        return mediaFileRepository.findByMediaTypeAndUploadedBy(mediaType, userId);
    }

    @Async
    public void processMediaAsync(MediaFile mediaFile) {
        try {
            switch (mediaFile.getMediaCategory()) {
                case IMAGE:
                    processImage(mediaFile);
                    break;
                case VIDEO:
                    processVideo(mediaFile);
                    break;
                case AUDIO:
                    processAudio(mediaFile);
                    break;
            }
            mediaFile.setProcessingStatus(ProcessingStatus.COMPLETED);
            mediaFileRepository.save(mediaFile);
        } catch (Exception e) {
            log.error("Failed to process media file: {}", mediaFile.getId(), e);
            mediaFile.setProcessingStatus(ProcessingStatus.FAILED);
            mediaFileRepository.save(mediaFile);
        }
    }

    private void processImage(MediaFile mediaFile) throws IOException {
        byte[] originalData = Files.readAllBytes(Paths.get(mediaFile.getFilePath()));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalData));

        if (image == null) {
            throw new MediaProcessingException("Failed to read image file");
        }

        // Set dimensions
        mediaFile.setWidth(image.getWidth());
        mediaFile.setHeight(image.getHeight());

        // Process based on media type
        if (mediaFile.getMediaType() == MediaType.AVATAR) {
            // Resize avatar to square
            BufferedImage processedImage = resizeToSquare(image, AVATAR_SIZE);
            saveProcessedImage(mediaFile, processedImage);

            mediaFile.setWidth(AVATAR_SIZE);
            mediaFile.setHeight(AVATAR_SIZE);
        } else if (image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
            // Resize if too large
            BufferedImage resizedImage = resizeIfNeeded(image, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
            saveProcessedImage(mediaFile, resizedImage);

            mediaFile.setWidth(resizedImage.getWidth());
            mediaFile.setHeight(resizedImage.getHeight());
        }
    }

    private void processVideo(MediaFile mediaFile) throws IOException {
        // Video processing logic here
        generateVideoThumbnail(mediaFile);
    }

    private void processAudio(MediaFile mediaFile) throws IOException {
        // Audio processing logic here
        log.info("Processing audio file: {}", mediaFile.getId());
    }

    private BufferedImage resizeToSquare(BufferedImage original, int size) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int cropSize = Math.min(originalWidth, originalHeight);

        // Crop to square first
        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;
        BufferedImage cropped = original.getSubimage(x, y, cropSize, cropSize);

        // Resize to target size
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, size, size, null);
        g.dispose();

        return resized;
    }

    private BufferedImage resizeIfNeeded(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return original;
        }

        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private void saveProcessedImage(MediaFile mediaFile, BufferedImage processedImage) throws IOException {
        Path processedDir = Paths.get(uploadDir, "processed");
        Files.createDirectories(processedDir);

        String processedFilename = "processed_" + mediaFile.getFilename();
        Path processedPath = processedDir.resolve(processedFilename);

        ImageIO.write(processedImage, "jpg", processedPath.toFile());
        mediaFile.setProcessedPath(processedPath.toString());
    }

    private void generateVideoThumbnail(MediaFile mediaFile) {
        try {
            Path thumbnailDir = Paths.get(uploadDir, "thumbnails");
            Files.createDirectories(thumbnailDir);

            String thumbnailFilename = "thumb_" + mediaFile.getFilename().replaceAll("\\.[^.]+$", ".jpg");
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
                !AUDIO_TYPES.contains(contentType)) {
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
        }
        return false;
    }

    private MediaCategory determineMediaCategory(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return MediaCategory.IMAGE;
        } else if (VIDEO_TYPES.contains(contentType)) {
            return MediaCategory.VIDEO;
        } else if (AUDIO_TYPES.contains(contentType)) {
            return MediaCategory.AUDIO;
        } else {
            throw new IllegalArgumentException("Unsupported media type: " + contentType);
        }
    }

    private Path createUploadDirectory(MediaType mediaType, MediaCategory category) throws IOException {
        String typeDir = mediaType.name().toLowerCase();
        String categoryDir = category.name().toLowerCase();
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
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public String getMediaUrl(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getId() == null) {
            return null;
        }

        String relativePath = String.format("/api/media/%s", mediaFile.getId());
        return baseUrl + relativePath;
    }

    @Transactional(readOnly = true)
    public Optional<MediaFile> findById(Long id) {
        return mediaFileRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MediaFile> findByEntityIdAndMediaType(Long entityId, MediaType mediaType) {
        return mediaFileRepository.findByEntityIdAndMediaType(entityId, mediaType);
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        mediaFileRepository.findById(mediaId).ifPresent(mediaFile -> {
            try {
                // Delete physical files
                Files.deleteIfExists(Paths.get(mediaFile.getFilePath()));
                if (mediaFile.getProcessedPath() != null) {
                    Files.deleteIfExists(Paths.get(mediaFile.getProcessedPath()));
                }
                if (mediaFile.getThumbnailPath() != null) {
                    Files.deleteIfExists(Paths.get(mediaFile.getThumbnailPath()));
                }

                // Delete from database
                mediaFileRepository.delete(mediaFile);
                log.info("Deleted media file: {}", mediaId);
            } catch (IOException e) {
                log.error("Failed to delete media file: {}", mediaId, e);
                throw new MediaProcessingException("Failed to delete media file", e);
            }
        });
    }

    public byte[] getMediaContent(Long mediaId) throws IOException {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

        Path filePath = mediaFile.getProcessedPath() != null
                ? Paths.get(mediaFile.getProcessedPath())
                : Paths.get(mediaFile.getFilePath());

        return Files.readAllBytes(filePath);
    }
}