// src/main/java/com/my/challenger/web/controllers/MediaController.java
package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.exception.MediaProcessingException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.exception.UnauthorizedException;
import com.my.challenger.repository.QuizQuestionRepository;
import com.my.challenger.service.MediaService;
import com.my.challenger.service.impl.MediaStreamingService;
import com.my.challenger.service.impl.MinioMediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Enhanced media upload and management endpoints")
public class MediaController {

    private final MediaService mediaService;
    private final MediaStreamingService streamingService;
    private final QuizQuestionRepository quizQuestionRepository;
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

    // =============================================================================
    // MEDIA STREAMING ENDPOINTS
    // =============================================================================

    @GetMapping("/question/{questionId}/stream")
    @Operation(summary = "Stream media for a question",
            description = "Proxy endpoint to stream media files for a question. Supports Range requests for video/audio seeking.")
    public ResponseEntity<Resource> streamQuestionMedia(
            @PathVariable Long questionId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Stream request for questionId={}, range={}", questionId, rangeHeader);

        // Look up the question's media
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        String s3Key = question.getQuestionMediaUrl();
        if (s3Key == null || s3Key.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Delegate to existing stream logic
        return streamMediaByS3Key(s3Key, rangeHeader, question.getQuestionMediaType());
    }

    @GetMapping("/question/{questionId}/thumbnail")
    @Operation(summary = "Stream thumbnail for a question")
    public ResponseEntity<Resource> streamQuestionThumbnail(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Thumbnail request for questionId={}", questionId);

        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        String thumbnailKey = question.getQuestionThumbnailUrl();
        if (thumbnailKey == null || thumbnailKey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return streamMediaByS3Key(thumbnailKey, null, com.my.challenger.entity.enums.MediaType.IMAGE);
    }

    @GetMapping("/stream/{mediaId}")
    @Operation(summary = "Stream media file by ID",
            description = "Proxy endpoint to stream media files. Supports Range requests for video/audio seeking.")
    public ResponseEntity<Resource> streamMedia(
            @PathVariable Long mediaId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Stream request for mediaId={}, range={}", mediaId, rangeHeader);

        MediaFile mediaFile = streamingService.getMediaFileById(mediaId);
        String s3Key = mediaFile.getS3Key();
        String contentType = mediaFile.getContentType();

        // Get file metadata for headers
        HeadObjectResponse metadata = streamingService.getObjectMetadata(s3Key);
        long contentLength = metadata.contentLength();
        String etag = metadata.eTag();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");
        headers.set(HttpHeaders.ETAG, etag);

        // Handle Range request for video/audio seeking
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(s3Key, rangeHeader, contentLength, contentType, headers);
        }

        // Full file stream
        headers.setContentLength(contentLength);
        InputStream inputStream = streamingService.streamMedia(s3Key);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/thumbnail/{mediaId}")
    @Operation(summary = "Stream thumbnail by media ID")
    public ResponseEntity<Resource> streamThumbnail(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Thumbnail request for mediaId={}", mediaId);

        MediaFile mediaFile = streamingService.getMediaFileById(mediaId);
        String thumbnailKey = mediaFile.getThumbnailPath();

        if (thumbnailKey == null || thumbnailKey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        HeadObjectResponse metadata = streamingService.getObjectMetadata(thumbnailKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // Thumbnails are typically JPEG
        headers.setContentLength(metadata.contentLength());
        headers.set(HttpHeaders.CACHE_CONTROL, "private, max-age=86400"); // Cache thumbnails longer

        InputStream inputStream = streamingService.streamMedia(thumbnailKey);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    // =============================================================================
    // PRIVATE HELPER METHODS
    // =============================================================================

    /**
     * Stream media by S3 key with optional range support
     */
    private ResponseEntity<Resource> streamMediaByS3Key(String s3Key, String rangeHeader, com.my.challenger.entity.enums.MediaType mediaType) {
        HeadObjectResponse metadata = streamingService.getObjectMetadata(s3Key);
        long contentLength = metadata.contentLength();
        String contentType = metadata.contentType() != null
                ? metadata.contentType()
                : getContentTypeFromMediaType(mediaType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(s3Key, rangeHeader, contentLength, contentType, headers);
        }

        headers.setContentLength(contentLength);
        InputStream inputStream = streamingService.streamMedia(s3Key);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    /**
     * Handle HTTP Range requests for video/audio seeking
     */
    private ResponseEntity<Resource> handleRangeRequest(
            String s3Key, String rangeHeader, long totalLength, String contentType, HttpHeaders headers) {

        try {
            // Parse range: "bytes=0-1000" or "bytes=0-"
            String rangeValue = rangeHeader.substring(6); // Remove "bytes="
            String[] ranges = rangeValue.split("-");

            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : totalLength - 1;

            // Validate range
            if (rangeStart > rangeEnd || rangeStart >= totalLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + totalLength)
                        .build();
            }

            // Limit chunk size (e.g., 5MB max per request)
            long maxChunkSize = 5 * 1024 * 1024;
            if (rangeEnd - rangeStart + 1 > maxChunkSize) {
                rangeEnd = rangeStart + maxChunkSize - 1;
            }

            long contentRangeLength = rangeEnd - rangeStart + 1;

            // Stream partial content from S3
            String s3Range = String.format("bytes=%d-%d", rangeStart, rangeEnd);
            ResponseInputStream<GetObjectResponse> responseStream =
                    streamingService.streamMediaRange(s3Key, s3Range);

            headers.setContentLength(contentRangeLength);
            headers.set(HttpHeaders.CONTENT_RANGE,
                    String.format("bytes %d-%d/%d", rangeStart, rangeEnd, totalLength));
            headers.setContentType(MediaType.parseMediaType(contentType));

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new InputStreamResource(responseStream));

        } catch (NumberFormatException e) {
            log.error("Invalid range header: {}", rangeHeader);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get content type from media type enum
     */
    private String getContentTypeFromMediaType(com.my.challenger.entity.enums.MediaType mediaType) {
        if (mediaType == null) return "application/octet-stream";
        return switch (mediaType) {
            case IMAGE -> "image/jpeg";
            case VIDEO -> "video/mp4";
            case AUDIO -> "audio/mpeg";
            default -> "application/octet-stream";
        };
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