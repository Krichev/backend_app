package com.my.challenger.service.impl;

import com.my.challenger.dto.media.AudioMetadata;
import com.my.challenger.dto.media.VideoMetadata;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
    private final MediaProcessingService mediaProcessingService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:52428800}") // 50MB default
    private long maxFileSize;

    @Value("${app.upload.max-video-size:209715200}") // 200MB for videos
    private long maxVideoSize;

    @Value("${app.upload.max-audio-size:52428800}") // 50MB for audio
    private long maxAudioSize;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Supported file types
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/avi", "video/mov", "video/quicktime"
    );

    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mp3", "audio/mpeg", "audio/wav", "audio/ogg", "audio/m4a", "audio/aac"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogg", "avi", "mov");
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

        // Create media file entity
        MediaFile mediaFile = new MediaFile();
        mediaFile.setOriginalFilename(file.getOriginalFilename());
        mediaFile.setFilename(uniqueFilename);
        mediaFile.setFilePath(filePath.toString());
        mediaFile.setContentType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setMediaType(mediaType);
        mediaFile.setMediaCategory(category);
        mediaFile.setEntityId(entityId);
        mediaFile.setUploadedBy(uploadedBy);
        mediaFile.setUploadedAt(LocalDateTime.now());
        mediaFile.setProcessingStatus(ProcessingStatus.PENDING);

        // Save to database
        mediaFile = mediaFileRepository.save(mediaFile);

        // Process asynchronously
        processMediaAsync(mediaFile);

        return mediaFile;
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

    private void processVideo(MediaFile mediaFile) {
        try {
            // Use FFmpeg for video processing
            VideoMetadata metadata = mediaProcessingService.extractVideoMetadata(mediaFile.getFilePath());

            mediaFile.setWidth(metadata.getWidth());
            mediaFile.setHeight(metadata.getHeight());
            mediaFile.setDurationSeconds(metadata.getDurationSeconds());
            mediaFile.setBitrate(metadata.getBitrate());
            mediaFile.setFrameRate(BigDecimal.valueOf(metadata.getFrameRate()));
            mediaFile.setResolution(metadata.getWidth() + "x" + metadata.getHeight());

            // Generate thumbnail at 5 seconds
            String thumbnailPath = mediaProcessingService.generateVideoThumbnail(
                    mediaFile.getFilePath(),
                    getUploadPath(mediaFile.getMediaType(), MediaCategory.IMAGE),
                    Duration.ofSeconds(5)
            );
            mediaFile.setThumbnailPath(thumbnailPath);

        } catch (Exception e) {
            log.error("Failed to process video: {}", mediaFile.getId(), e);
            throw new MediaProcessingException("Video processing failed", e);
        }
    }

    private void processAudio(MediaFile mediaFile) {
        try {
            AudioMetadata metadata = mediaProcessingService.extractAudioMetadata(mediaFile.getFilePath());

            mediaFile.setDurationSeconds(metadata.getDurationSeconds());
            mediaFile.setBitrate(metadata.getBitrate());

        } catch (Exception e) {
            log.error("Failed to process audio: {}", mediaFile.getId(), e);
            throw new MediaProcessingException("Audio processing failed", e);
        }
    }

    private BufferedImage resizeToSquare(BufferedImage img, int size) {
        int width = img.getWidth();
        int height = img.getHeight();

        // Calculate crop dimensions
        int cropSize = Math.min(width, height);
        int x = (width - cropSize) / 2;
        int y = (height - cropSize) / 2;

        // Crop to square
        BufferedImage croppedImage = img.getSubimage(x, y, cropSize, cropSize);

        // Resize
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(croppedImage, 0, 0, size, size, null);
        g.dispose();

        return resized;
    }

    private BufferedImage resizeIfNeeded(BufferedImage img, int maxWidth, int maxHeight) {
        int width = img.getWidth();
        int height = img.getHeight();

        // Calculate scaling factor
        double scaleFactor = Math.min(
                (double) maxWidth / width,
                (double) maxHeight / height
        );

        if (scaleFactor >= 1.0) {
            return img; // No resize needed
        }

        int newWidth = (int) (width * scaleFactor);
        int newHeight = (int) (height * scaleFactor);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private void saveProcessedImage(MediaFile mediaFile, BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] bytes = baos.toByteArray();

        Path processedPath = Paths.get(mediaFile.getFilePath().replace(".", "_processed."));
        Files.write(processedPath, bytes);
        mediaFile.setProcessedPath(processedPath.toString());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type is not specified");
        }

        // Check file size based on type
        if (VIDEO_TYPES.contains(contentType) && file.getSize() > maxVideoSize) {
            throw new IllegalArgumentException("Video file size exceeds maximum limit of " + maxVideoSize + " bytes");
        } else if (AUDIO_TYPES.contains(contentType) && file.getSize() > maxAudioSize) {
            throw new IllegalArgumentException("Audio file size exceeds maximum limit of " + maxAudioSize + " bytes");
        } else if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + maxFileSize + " bytes");
        }

        // Validate file extension
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

    private Path getUploadPath(MediaType mediaType, MediaCategory category) {
        return Paths.get(uploadDir, mediaType.name().toLowerCase(), category.name().toLowerCase());
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
        if (mediaFile == null || mediaFile.getFilename() == null) {
            return null;
        }

        String relativePath = String.format("/api/media/file/%s", mediaFile.getId());
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