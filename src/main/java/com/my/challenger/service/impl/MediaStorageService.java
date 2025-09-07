package com.my.challenger.service.impl;

import com.my.challenger.dto.media.VideoMetadata;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import com.my.challenger.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.model.AudioMetadata;

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

    public MediaFile saveMedia(MultipartFile file, MediaType mediaType, Long entityId, Long uploadedBy)
            throws IOException {

        MediaCategory category = validateAndDetectMediaCategory(file);

        // Create upload directory
        Path uploadPath = createUploadDirectory(mediaType, category);

        // Generate unique filename
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(filename);

        // Save initial file
        Files.copy(file.getInputStream(), filePath);

        // Create media record
        MediaFile mediaFile = MediaFile.builder()
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .mediaCategory(category)
                .uploadedBy(uploadedBy)
                .mediaType(mediaType)
                .entityId(entityId)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        // Remove old media if updating avatar
        if (mediaType == MediaType.AVATAR && entityId != null) {
            deleteExistingMedia(entityId, mediaType);
        }

        mediaFile = mediaFileRepository.save(mediaFile);

        // Process asynchronously
        processMediaAsync(mediaFile);

        return mediaFile;
    }

    private MediaCategory validateAndDetectMediaCategory(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IOException("Unable to determine file type");
        }

        MediaCategory category;
        long maxSize;

        if (IMAGE_TYPES.contains(contentType)) {
            category = MediaCategory.IMAGE;
            maxSize = maxFileSize;
        } else if (VIDEO_TYPES.contains(contentType)) {
            category = MediaCategory.VIDEO;
            maxSize = maxVideoSize;
        } else if (AUDIO_TYPES.contains(contentType)) {
            category = MediaCategory.AUDIO;
            maxSize = maxAudioSize;
        } else {
            throw new IOException("File type not supported: " + contentType);
        }

        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum allowed size for " + category);
        }

        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        Set<String> allowedExtensions = switch (category) {
            case IMAGE -> IMAGE_EXTENSIONS;
            case VIDEO -> VIDEO_EXTENSIONS;
            case AUDIO -> AUDIO_EXTENSIONS;
        };

        if (!allowedExtensions.contains(extension)) {
            throw new IOException("File extension not allowed: " + extension);
        }

        return category;
    }

    @Async
    public void processMediaAsync(MediaFile mediaFile) {
        try {
            mediaFile.setProcessingStatus(ProcessingStatus.PROCESSING);
            mediaFileRepository.save(mediaFile);

            switch (mediaFile.getMediaCategory()) {
                case IMAGE -> processImage(mediaFile);
                case VIDEO -> processVideo(mediaFile);
                case AUDIO -> processAudio(mediaFile);
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

            // Generate thumbnail
            String thumbnailPath = mediaProcessingService.generateVideoThumbnail(
                    mediaFile.getFilePath(),
                    getUploadPath(mediaFile.getMediaType(), MediaCategory.IMAGE)
            );
            mediaFile.setThumbnailPath(thumbnailPath);

        } catch (Exception e) {
            log.error("Failed to process video: {}", mediaFile.getId(), e);
        }
    }

    private void processAudio(MediaFile mediaFile) {
        try {
            AudioMetadata metadata = mediaProcessingService.extractAudioMetadata(mediaFile.getFilePath());

            mediaFile.setDurationSeconds(metadata.getDurationSeconds());
            mediaFile.setBitrate(metadata.getBitrate());

        } catch (Exception e) {
            log.error("Failed to process audio: {}", mediaFile.getId(), e);
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

    private BufferedImage resizeToSquare(BufferedImage originalImage, int size) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Calculate crop dimensions (center crop to square)
        int cropSize = Math.min(width, height);
        int x = (width - cropSize) / 2;
        int y = (height - cropSize) / 2;

        // Crop to square
        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

        // Resize to target size
        BufferedImage resizedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    private void saveProcessedImage(MediaFile mediaFile, BufferedImage processedImage) throws IOException {
        String format = getImageFormat(mediaFile.getMimeType());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(processedImage, format, baos);

        Files.write(Paths.get(mediaFile.getFilePath()), baos.toByteArray());
        mediaFile.setFileSize((long) baos.size());
    }

    private String getImageFormat(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    // Public methods for retrieval
    public Optional<MediaFile> getMedia(Long id) {
        return mediaFileRepository.findById(id);
    }

    public Optional<MediaFile> getMediaByEntityAndType(Long entityId, MediaType mediaType) {
        return mediaFileRepository.findByEntityIdAndMediaType(entityId, mediaType);
    }

    public List<MediaFile> getMediaByTypeAndUser(MediaType mediaType, Long userId) {
        return mediaFileRepository.findByMediaTypeAndUploadedBy(mediaType, userId);
    }

    public byte[] getMediaData(MediaFile mediaFile) throws IOException {
        return Files.readAllBytes(Paths.get(mediaFile.getFilePath()));
    }

    public byte[] getThumbnailData(MediaFile mediaFile) throws IOException {
        if (mediaFile.getThumbnailPath() != null) {
            return Files.readAllBytes(Paths.get(mediaFile.getThumbnailPath()));
        }
        return null;
    }

    public void deleteMedia(Long mediaId) throws IOException {
        Optional<MediaFile> mediaOpt = mediaFileRepository.findById(mediaId);
        if (mediaOpt.isPresent()) {
            MediaFile mediaFile = mediaOpt.get();

            // Delete main file
            Files.deleteIfExists(Paths.get(mediaFile.getFilePath()));

            // Delete thumbnail if exists
            if (mediaFile.getThumbnailPath() != null) {
                Files.deleteIfExists(Paths.get(mediaFile.getThumbnailPath()));
            }

            mediaFileRepository.delete(mediaFile);
        }
    }

    private void deleteExistingMedia(Long entityId, MediaType mediaType) {
        mediaFileRepository.findByEntityIdAndMediaType(entityId, mediaType)
                .ifPresent(existingMedia -> {
                    try {
                        deleteMedia(existingMedia.getId());
                    } catch (IOException e) {
                        log.warn("Failed to delete existing media: {}", e.getMessage());
                    }
                });
    }

    public String getMediaUrl(MediaFile mediaFile) {
        return "/api/media/" + mediaFile.getId();
    }

    public String getThumbnailUrl(MediaFile mediaFile) {
        if (mediaFile.getThumbnailPath() != null) {
            return "/api/media/" + mediaFile.getId() + "/thumbnail";
        }
        return null;
    }
}