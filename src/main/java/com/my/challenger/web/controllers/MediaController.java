// src/main/java/com/my/challenger/web/controllers/MediaController.java
package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Enhanced media upload and management endpoints")
public class MediaController {

    private final MediaStorageService mediaStorageService;
    private final UserRepository userRepository;

    @PostMapping("/upload/quiz-media")
    @Operation(summary = "Upload media for quiz question (image, video, or audio)")
    public ResponseEntity<Map<String, Object>> uploadQuizMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) String questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Parse questionId - handle both Long and String (for temp IDs)
            Long entityId = null;
            if (questionId != null && !questionId.startsWith("temp_")) {
                try {
                    entityId = Long.parseLong(questionId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid question ID format: {}, treating as temporary", questionId);
                }
            }

            // Store media with QUIZ_QUESTION category
            MediaFile mediaFile = mediaStorageService.storeMedia(
                    file,
                    entityId,
                    MediaCategory.QUIZ_QUESTION,
                    user.getId()
            );

            log.info("Quiz media uploaded successfully: {} for question: {}",
                    mediaFile.getId(), questionId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                            mediaStorageService.getThumbnailUrl(mediaFile) : "",
                    "mediaType", mediaFile.getMediaType().toString(),
                    "processingStatus", mediaFile.getProcessingStatus().toString(),
                    "message", "Media uploaded successfully"
            ));
        } catch (Exception e) {
            log.error("Error uploading quiz media", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to upload media: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/upload/avatar")
    @Operation(summary = "Upload avatar media")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            MediaFile mediaFile = mediaStorageService.storeMedia(
                    file,
                    user.getId(),
                    MediaCategory.AVATAR,
                    user.getId()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                            mediaStorageService.getThumbnailUrl(mediaFile) : "",
                    "message", "Avatar uploaded successfully"
            ));
        } catch (Exception e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to upload avatar: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/url/{mediaId}")
    @Operation(summary = "Get media URL by media ID")
    public ResponseEntity<Map<String, String>> getMediaUrl(@PathVariable Long mediaId) {
        try {
            MediaFile mediaFile = mediaStorageService.getMediaFileById(mediaId);
            return ResponseEntity.ok(Map.of(
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                            mediaStorageService.getThumbnailUrl(mediaFile) : ""
            ));
        } catch (Exception e) {
            log.error("Error getting media URL", e);
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Media not found"
            ));
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file")
    public ResponseEntity<Map<String, Object>> deleteMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            mediaStorageService.deleteMedia(mediaId, user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Media deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting media", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to delete media: " + e.getMessage()
            ));
        }
    }
}