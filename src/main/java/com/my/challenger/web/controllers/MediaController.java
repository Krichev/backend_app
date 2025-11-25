// src/main/java/com/my/challenger/web/controllers/MediaController.java
package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.exception.UnauthorizedException;
import com.my.challenger.service.MediaService;
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

    private final MediaService mediaService;
    private final MinioMediaStorageService storageService; // Keep for backward compatibility

    @PostMapping("/upload/quiz-media")
    @Operation(summary = "Upload media for quiz question",
            description = "Upload image, video, or audio file for a quiz question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or request"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadQuizMedia(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Delegate to service
            MediaFile mediaFile = mediaService.uploadQuizMedia(file, userDetails.getUsername());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("fileName", mediaFile.getFilename());
            response.put("fileSize", mediaFile.getFileSize());
            response.put("mediaType", mediaFile.getMediaType().toString());
            response.put("mediaUrl", storageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    storageService.getThumbnailUrl(mediaFile) : "");
            response.put("processingStatus", mediaFile.getProcessingStatus().toString());
            response.put("message", "Media uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (MediaProcessingException e) {
            log.error("❌ Media processing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error during media upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload media"));
        }
    }

    @PostMapping("/upload/avatar")
    @Operation(summary = "Upload user avatar",
            description = "Upload avatar image for the authenticated user (max 10MB)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @Parameter(description = "Avatar image file", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Delegate to service
            MediaFile mediaFile = mediaService.uploadAvatar(file, userDetails.getUsername());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("mediaUrl", storageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    storageService.getThumbnailUrl(mediaFile) : "");
            response.put("message", "Avatar uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (MediaProcessingException e) {
            log.error("❌ Media processing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error during avatar upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload avatar"));
        }
    }

    @GetMapping("/url/{mediaId}")
    @Operation(summary = "Get media URL by media ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media URL retrieved"),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> getMediaUrl(
            @Parameter(description = "Media ID", required = true)
            @PathVariable Long mediaId) {

        try {
            Map<String, String> urls = mediaService.getMediaUrls(mediaId);
            return ResponseEntity.ok(urls);

        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Media not found"));
        } catch (Exception e) {
            log.error("❌ Error getting media URL: {}", mediaId, e);
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
            MediaFile mediaFile = mediaService.getMediaById(mediaId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mediaId", mediaFile.getId());
            response.put("fileName", mediaFile.getFilename());
            response.put("fileSize", mediaFile.getFileSize());
            response.put("mediaType", mediaFile.getMediaType().toString());
            response.put("mediaUrl", storageService.getMediaUrl(mediaFile));
            response.put("thumbnailUrl", mediaFile.getThumbnailPath() != null ?
                    storageService.getThumbnailUrl(mediaFile) : "");
            response.put("processingStatus", mediaFile.getProcessingStatus().toString());
            response.put("uploadedAt", mediaFile.getCreatedAt().toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Media not found"));
        } catch (Exception e) {
            log.error("❌ Error getting media details: {}", mediaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve media details"));
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Media deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> deleteMedia(
            @Parameter(description = "Media ID", required = true)
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Delegate to service
            mediaService.deleteMedia(mediaId, userDetails.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Media deleted successfully");

            return ResponseEntity.ok(response);

        } catch (UnauthorizedException e) {
            log.error("❌ Unauthorized delete attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("❌ Media not found: {}", mediaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Media not found"));
        } catch (Exception e) {
            log.error("❌ Error deleting media: {}", mediaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete media"));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}