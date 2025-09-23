package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.service.impl.MediaStorageService;
import com.my.challenger.service.impl.AdvancedS3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Enhanced media upload and management endpoints")
public class MediaController {

    private final MediaStorageService mediaStorageService;
    private final AdvancedS3Service advancedS3Service;

    @PostMapping("/upload/quiz-media")
    @Operation(summary = "Upload media for quiz question (image, video, or audio)")
    public ResponseEntity<Map<String, Object>> uploadQuizMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) String questionId,
            @RequestParam(value = "mediaCategory", defaultValue = "QUIZ_QUESTION") String mediaCategory,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = getUserIdFromDetails(userDetails);

            // Parse questionId - handle both Long and String (for temp IDs)
            Long entityId = null;
            if (questionId != null && !questionId.startsWith("temp_")) {
                try {
                    entityId = Long.parseLong(questionId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid question ID format: {}", questionId);
                }
            }

            MediaFile mediaFile = mediaStorageService.saveMedia(
                    file,
                    MediaType.QUIZ_QUESTION,
                    entityId,
                    userId
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaFile.getThumbnailPath() != null ? mediaFile.getThumbnailPath() : "",
                    "mediaType", mediaFile.getMediaCategory().name(),
                    "fileType", mediaFile.getContentType(),
                    "processingStatus", mediaFile.getProcessingStatus().name(),
                    "fileSize", mediaFile.getFileSize(),
                    "duration", mediaFile.getDurationSeconds(),
                    "message", "Media uploaded successfully"
            ));
        } catch (IOException e) {
            log.error("Error uploading quiz media", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to upload media: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error uploading quiz media", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "An unexpected error occurred"
            ));
        }
    }

    @PostMapping("/presigned-upload")
    @Operation(summary = "Get presigned URL for direct S3 upload")
    public ResponseEntity<Map<String, Object>> getPresignedUploadUrl(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String fileName = request.get("fileName");
            String fileType = request.get("fileType");
            String category = request.getOrDefault("category", "QUIZ_QUESTION");

            if (fileName == null || fileType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "fileName and fileType are required"
                ));
            }

            // Generate unique key for S3
            String s3Key = generateS3Key(category, fileName);

            // Get presigned URL (valid for 15 minutes)
            String uploadUrl = advancedS3Service.generateUploadPresignedUrl(s3Key, fileType, 15);
            String mediaUrl = constructMediaUrl(s3Key);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "uploadUrl", uploadUrl,
                    "mediaUrl", mediaUrl,
                    "s3Key", s3Key
            ));
        } catch (Exception e) {
            log.error("Error generating presigned URL", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to generate upload URL"
            ));
        }
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media information by ID")
    public ResponseEntity<Map<String, Object>> getMediaInfo(
            @PathVariable String mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Optional<MediaFile> mediaFileOptional = mediaStorageService.getMedia(Long.parseLong(mediaId));

            if (mediaFileOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            MediaFile mediaFile = mediaFileOptional.get();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaFile.getThumbnailPath() != null ? mediaFile.getThumbnailPath() : "",
                    "mediaType", mediaFile.getMediaCategory().name(),
                    "fileType", mediaFile.getContentType(),
                    "fileName", mediaFile.getFilename(),
                    "fileSize", mediaFile.getFileSize(),
                    "duration", mediaFile.getDurationSeconds(),
                    "processingStatus", mediaFile.getProcessingStatus().name()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid media ID format"
            ));
        } catch (Exception e) {
            log.error("Error getting media info", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to get media info"
            ));
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file")
    public ResponseEntity<Map<String, Object>> deleteMedia(
            @PathVariable String mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = getUserIdFromDetails(userDetails);
            boolean deleted = mediaStorageService.deleteMedia(Long.parseLong(mediaId));

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Media deleted successfully"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid media ID format"
            ));
        } catch (Exception e) {
            log.error("Error deleting media", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to delete media"
            ));
        }
    }

    // Helper methods
    private Long getUserIdFromDetails(UserDetails userDetails) {
        // Implementation depends on your UserDetails implementation
        // This is a placeholder - implement according to your auth system
        return 1L; // Replace with actual user ID extraction
    }

    private String generateS3Key(String category, String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(fileName);
        return String.format("%s/%s_%s.%s",
                category.toLowerCase(),
                timestamp,
                generateRandomString(8),
                extension);
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "bin";
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return result.toString();
    }

    private String constructMediaUrl(String s3Key) {
        // Construct the media URL based on your S3 configuration
        // This could be CloudFront URL or direct S3 URL
        return String.format("https://your-bucket.s3.region.amazonaws.com/%s", s3Key);
    }
}