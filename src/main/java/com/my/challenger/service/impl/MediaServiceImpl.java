// src/main/java/com/my/challenger/service/impl/MediaServiceImpl.java
package com.my.challenger.service.impl;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.exception.UnauthorizedException;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final MinioMediaStorageService storageService;
    private final UserRepository userRepository;

    private static final long MAX_QUIZ_MEDIA_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    @Transactional
    public MediaFile uploadQuizMedia(MultipartFile file, String username) {
        log.info("📤 Uploading quiz media - File: {}, User: {}",
                file.getOriginalFilename(), username);

        // Validate file
        validateQuizMedia(file);

        // Get user within transaction
        User user = getUserByUsername(username);

        try {
            // Store media
            MediaFile mediaFile = storageService.storeMedia(
                    file,
                    null, // entityId - will be set later when question is created
                    MediaCategory.QUIZ_QUESTION,
                    user.getId()
            );

            log.info("✅ Quiz media uploaded successfully - ID: {}, User: {}",
                    mediaFile.getId(), username);

            return mediaFile;

        } catch (Exception e) {
            log.error("❌ Failed to upload quiz media for user: {}", username, e);
            throw new MediaProcessingException("Failed to upload quiz media: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> uploadChallengeAudio(Long challengeId, MultipartFile file, Long userId) {
        log.info("📤 Uploading challenge audio - ID: {}, File: {}, User ID: {}",
                challengeId, file.getOriginalFilename(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Use existing validation
        validateQuizMedia(file);

        try {
            // Reuse QUIZ_QUESTION category for now
            MediaFile mediaFile = storageService.storeMedia(
                    file,
                    challengeId,
                    MediaCategory.QUIZ_QUESTION,
                    user.getId()
            );

            log.info("✅ Challenge audio uploaded successfully - ID: {}, Challenge: {}",
                    mediaFile.getId(), challengeId);

            // Construct response map
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("mediaUrl", storageService.getMediaUrl(mediaFile));
            response.put("fileName", mediaFile.getFilename());
            response.put("fileSize", mediaFile.getFileSize());
            response.put("duration", mediaFile.getDurationSeconds());
            response.put("mediaType", mediaFile.getMediaType().toString());
            response.put("processingStatus", mediaFile.getProcessingStatus().toString());
            response.put("message", "Audio uploaded successfully");

            return response;

        } catch (Exception e) {
            log.error("❌ Failed to upload challenge audio for challenge: {}", challengeId, e);
            throw new MediaProcessingException("Failed to upload challenge audio: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public MediaFile uploadAvatar(MultipartFile file, String username) {
        log.info("📤 Uploading avatar - File: {}, User: {}",
                file.getOriginalFilename(), username);

        // Validate avatar
        validateAvatar(file);

        // Get user within transaction
        User user = getUserByUsername(username);

        try {
            // Store avatar
            MediaFile mediaFile = storageService.storeMedia(
                    file,
                    user.getId(),
                    MediaCategory.AVATAR,
                    user.getId()
            );

            log.info("✅ Avatar uploaded successfully - ID: {}, User: {}",
                    mediaFile.getId(), username);

            return mediaFile;

        } catch (Exception e) {
            log.error("❌ Failed to upload avatar for user: {}", username, e);
            throw new MediaProcessingException("Failed to upload avatar: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile getMediaById(Long mediaId) {
        return storageService.getMediaFileById(mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getMediaUrls(Long mediaId) {
        MediaFile mediaFile = storageService.getMediaFileById(mediaId);

        Map<String, String> urls = new HashMap<>();
        urls.put("mediaUrl", storageService.getMediaUrl(mediaFile));

        if (mediaFile.getThumbnailPath() != null) {
            urls.put("thumbnailUrl", storageService.getThumbnailUrl(mediaFile));
        }

        return urls;
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId, String username) {
        log.info("🗑️ Deleting media - ID: {}, User: {}", mediaId, username);

        // Get user within transaction
        User user = getUserByUsername(username);

        // Get media file
        MediaFile mediaFile = storageService.getMediaFileById(mediaId);

        // Check authorization
        if (!mediaFile.getUploadedBy().equals(user.getId())) {
            log.warn("❌ Unauthorized delete attempt - Media: {}, User: {}", mediaId, username);
            throw new UnauthorizedException("You are not authorized to delete this media file");
        }

        // Delete via storage service
        storageService.deleteMedia(mediaId, user.getId());

        log.info("✅ Media deleted successfully - ID: {}, User: {}", mediaId, username);
    }

    /**
     * Get user by username within transaction boundary
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Validate quiz media file
     */
    private void validateQuizMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_QUIZ_MEDIA_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            MAX_QUIZ_MEDIA_SIZE / (1024 * 1024)));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type cannot be determined");
        }

        // Validate allowed media types
        if (!isAllowedMediaType(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed types: images, videos, and audio files");
        }
    }

    /**
     * Validate avatar file
     */
    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Avatar size exceeds maximum of %d MB",
                            MAX_AVATAR_SIZE / (1024 * 1024)));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image file");
        }
    }

    /**
     * Check if content type is allowed
     */
    private boolean isAllowedMediaType(String contentType) {
        return contentType.startsWith("image/") ||
               contentType.startsWith("video/") ||
               contentType.startsWith("audio/");
    }
}
