// src/main/java/com/my/challenger/web/controllers/MediaController.java
package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.MinioMediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Enhanced media upload and management endpoints")
public class MediaController {

    private final MinioMediaStorageService mediaStorageService;
    private final UserRepository userRepository;

    @PostMapping("/upload/quiz-media")
    @Operation(summary = "Upload media for quiz question",
            description = "Upload image, video, or audio file for a quiz question. Supports progress tracking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadQuizMedia(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Question ID (optional, can be temporary ID)")
            @RequestParam(value = "questionId", required = false) String questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received quiz media upload request from user: {}, file: {}, questionId: {}",
                userDetails.getUsername(), file.getOriginalFilename(), questionId);

        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("Empty file received from user: {}", userDetails.getUsername());
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            // Check file size (max 100MB)
            long maxFileSize = 100 * 1024 * 1024; // 100MB
            if (file.getSize() > maxFileSize) {
                log.warn("File too large: {} bytes from user: {}", file.getSize(), userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(createErrorResponse("File too large. Maximum size is 100MB"));
            }

            // Validate content type
            String contentType = file.getContentType();
            if (contentType == null || !isValidMediaType(contentType)) {
                log.warn("Unsupported media type: {} from user: {}", contentType, userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(createErrorResponse("Unsupported file type: " + contentType));
            }

            // Get user
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

            // Parse questionId - handle both Long and String (for temp IDs)
            Long entityId = null;
            if (questionId != null && !questionId.trim().isEmpty() && !questionId.startsWith("temp_")) {
                try {
                    entityId = Long.parseLong(questionId);
                    log.debug("Parsed question ID: {}", entityId);
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

            log.info("✅ Quiz media uploaded successfully - ID: {}, Type: {}, User: {}, Question: {}",
                    mediaFile.getId(), mediaFile.getMediaType(), user.getUsername(), questionId);

            // Build success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("mediaUrl", mediaStorageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    mediaStorageService.getThumbnailUrl(mediaFile) : "");
            response.put("mediaType", mediaFile.getMediaType().toString());
            response.put("processingStatus", mediaFile.getProcessingStatus().toString());
            response.put("fileName", mediaFile.getFilename());
            response.put("fileSize", mediaFile.getFileSize());
            response.put("message", "Media uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid argument during quiz media upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error uploading quiz media from user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload media: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/avatar")
    @Operation(summary = "Upload avatar/profile picture")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @Parameter(description = "Avatar image file", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received avatar upload request from user: {}, file: {}",
                userDetails.getUsername(), file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            // Check file size for avatar (max 10MB)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxFileSize) {
                log.warn("Avatar file too large: {} bytes", file.getSize());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(createErrorResponse("Avatar file too large. Maximum size is 10MB"));
            }

            // Validate it's an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Non-image file submitted as avatar: {}", contentType);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(createErrorResponse("Avatar must be an image file"));
            }

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

            MediaFile mediaFile = mediaStorageService.storeMedia(
                    file,
                    user.getId(),
                    MediaCategory.AVATAR,
                    user.getId()
            );

            log.info("✅ Avatar uploaded successfully - ID: {}, User: {}", mediaFile.getId(), user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("mediaUrl", mediaStorageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    mediaStorageService.getThumbnailUrl(mediaFile) : "");
            response.put("message", "Avatar uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid argument during avatar upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error uploading avatar for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload avatar: " + e.getMessage()));
        }
    }

    @GetMapping("/url/{mediaId}")
    @Operation(summary = "Get media URL by media ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media URL retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> getMediaUrl(
            @Parameter(description = "Media ID", required = true)
            @PathVariable Long mediaId) {
        try {
            MediaFile mediaFile = mediaStorageService.getMediaFileById(mediaId);

            Map<String, String> response = new HashMap<>();
            response.put("mediaUrl", mediaStorageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    mediaStorageService.getThumbnailUrl(mediaFile) : "");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found with ID: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Media not found with ID: " + mediaId));
        } catch (Exception e) {
            log.error("❌ Error getting media URL for ID: {}", mediaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve media URL"));
        }
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media details retrieved"),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getMediaDetails(
            @Parameter(description = "Media ID", required = true)
            @PathVariable Long mediaId) {
        try {
            MediaFile mediaFile = mediaStorageService.getMediaFileById(mediaId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("fileName", mediaFile.getFilename());
            response.put("fileSize", mediaFile.getFileSize());
            response.put("mediaType", mediaFile.getMediaType().toString());
            response.put("mediaUrl", mediaStorageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    mediaStorageService.getThumbnailUrl(mediaFile) : "");
            response.put("processingStatus", mediaFile.getProcessingStatus().toString());
            response.put("uploadedAt", mediaFile.getCreatedAt().toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found with ID: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Media not found with ID: " + mediaId));
        } catch (Exception e) {
            log.error("❌ Error getting media details for ID: {}", mediaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve media details"));
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this media"),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> deleteMedia(
            @Parameter(description = "Media ID to delete", required = true)
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Delete media request - ID: {}, User: {}", mediaId, userDetails.getUsername());

        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

            mediaStorageService.deleteMedia(mediaId, user.getId());

            log.info("✅ Media deleted successfully - ID: {}, User: {}", mediaId, user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Media deleted successfully"
            ));

        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found or user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("❌ Unauthorized delete attempt - Media ID: {}, User: {}",
                    mediaId, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You are not authorized to delete this media"));
        } catch (Exception e) {
            log.error("❌ Error deleting media - ID: {}, User: {}",
                    mediaId, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete media: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create consistent error responses
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        return response;
    }

    /**
     * Validate if the content type is a supported media type
     */
    private boolean isValidMediaType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();

        // Image types
        if (lowerContentType.startsWith("image/jpeg") ||
                lowerContentType.startsWith("image/jpg") ||
                lowerContentType.startsWith("image/png") ||
                lowerContentType.startsWith("image/gif") ||
                lowerContentType.startsWith("image/webp")) {
            return true;
        }

        // Video types
        if (lowerContentType.startsWith("video/mp4") ||
                lowerContentType.startsWith("video/mov") ||
                lowerContentType.startsWith("video/avi") ||
                lowerContentType.startsWith("video/quicktime")) {
            return true;
        }

        // Audio types
        if (lowerContentType.startsWith("audio/mp3") ||
                lowerContentType.startsWith("audio/mpeg") ||
                lowerContentType.startsWith("audio/wav") ||
                lowerContentType.startsWith("audio/aac") ||
                lowerContentType.startsWith("audio/m4a") ||
                lowerContentType.startsWith("audio/ogg")) {
            return true;
        }

        return false;
    }
}