package com.my.challenger.web.controllers;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.service.impl.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
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
@Tag(name = "Media Management", description = "Media upload and management endpoints")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping("/upload/avatar")
    @Operation(summary = "Upload user avatar (image or video)")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = getUserIdFromDetails(userDetails);
            MediaFile mediaFile = mediaStorageService.saveMedia(file, MediaType.AVATAR, userId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "mediaType", mediaFile.getMediaCategory().name(),
                    "processingStatus", mediaFile.getProcessingStatus().name(),
                    "message", "Avatar uploaded successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to upload avatar: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/upload/quiz-media")
    @Operation(summary = "Upload media for quiz question (image, video, or audio)")
    public ResponseEntity<Map<String, Object>> uploadQuizMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionId") Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = getUserIdFromDetails(userDetails);
            MediaFile mediaFile = mediaStorageService.saveMedia(file, MediaType.QUIZ_QUESTION, questionId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mediaId", mediaFile.getId(),
                    "mediaUrl", mediaStorageService.getMediaUrl(mediaFile),
                    "thumbnailUrl", mediaStorageService.getThumbnailUrl(mediaFile),
                    "mediaType", mediaFile.getMediaCategory().name(),
                    "processingStatus", mediaFile.getProcessingStatus().name(),
                    "message", "Quiz media uploaded successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to upload quiz media: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media file by ID")
    public ResponseEntity<ByteArrayResource> getMedia(@PathVariable Long mediaId) {
        try {
            Optional<MediaFile> mediaOpt = mediaStorageService.getMedia(mediaId);
            if (mediaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MediaFile mediaFile = mediaOpt.get();
            byte[] mediaData = mediaStorageService.getMediaData(mediaFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(mediaFile.getMimeType()));
            headers.setContentLength(mediaData.length);

            // Set appropriate cache headers based on media type
            if (mediaFile.getMediaType() == MediaType.AVATAR) {
                headers.setCacheControl("public, max-age=86400"); // 1 day for avatars
            } else {
                headers.setCacheControl("public, max-age=31536000"); // 1 year for other media
            }

            // Set content disposition for downloads
            if (mediaFile.getMediaCategory().name().equals("AUDIO")) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + mediaFile.getOriginalFilename() + "\"");
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(mediaData));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get thumbnail for video media")
    public ResponseEntity<ByteArrayResource> getMediaThumbnail(@PathVariable Long mediaId) {
        try {
            Optional<MediaFile> mediaOpt = mediaStorageService.getMedia(mediaId);
            if (mediaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MediaFile mediaFile = mediaOpt.get();
            byte[] thumbnailData = mediaStorageService.getThumbnailData(mediaFile);

            if (thumbnailData == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
            headers.setContentLength(thumbnailData.length);
            headers.setCacheControl("public, max-age=86400");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(thumbnailData));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/avatar")
    @Operation(summary = "Get user avatar")
    public ResponseEntity<ByteArrayResource> getUserAvatar(@PathVariable Long userId) {
        Optional<MediaFile> avatarMedia = mediaStorageService.getMediaByEntityAndType(userId, MediaType.AVATAR);

        if (avatarMedia.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            MediaFile mediaFile = avatarMedia.get();
            byte[] mediaData = mediaStorageService.getMediaData(mediaFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(mediaFile.getMimeType()));
            headers.setContentLength(mediaData.length);
            headers.setCacheControl("public, max-age=86400");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(mediaData));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/storage-usage")
    @Operation(summary = "Get user storage usage statistics")
    public ResponseEntity<Map<String, Object>> getUserStorageUsage(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Add authorization check here
        Long totalFiles = mediaStorageService.getMediaByTypeAndUser(MediaType.AVATAR, userId).size() +
                mediaStorageService.getMediaByTypeAndUser(MediaType.QUIZ_QUESTION, userId).size();

        // Calculate total storage (implement in repository)
        return ResponseEntity.ok(Map.of(
                "totalFiles", totalFiles,
                "totalStorageBytes", 0L, // Implement this
                "storageLimit", 1073741824L // 1GB limit
        ));
    }

    @PostMapping("/{mediaId}/convert")
    @Operation(summary = "Convert video to different format/quality")
    public ResponseEntity<Map<String, Object>> convertVideo(
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "mp4") String format,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer bitrate,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Optional<MediaFile> mediaOpt = mediaStorageService.getMedia(mediaId);
            if (mediaOpt.isEmpty() || mediaOpt.get().getMediaCategory() != MediaCategory.VIDEO) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Video not found"
                ));
            }

            // Add authorization check here

            VideoConversionOptions options = new VideoConversionOptions();
            options.setFormat(format);
            options.setWidth(width);
            options.setHeight(height);
            options.setBitrate(bitrate);

            // Start async conversion
            // This would be implemented in MediaStorageService

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video conversion started",
                    "conversionId", "conv_" + System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to convert video: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{mediaId}/extract-audio")
    @Operation(summary = "Extract audio from video")
    public ResponseEntity<Map<String, Object>> extractAudio(
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "mp3") String format,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Optional<MediaFile> mediaOpt = mediaStorageService.getMedia(mediaId);
            if (mediaOpt.isEmpty() || mediaOpt.get().getMediaCategory() != MediaCategory.VIDEO) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Video not found"
                ));
            }

            // Add authorization check here

            // Start async audio extraction
            // This would be implemented in MediaStorageService

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Audio extraction started",
                    "extractionId", "extract_" + System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to extract audio: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{mediaId}/metadata")
    @Operation(summary = "Get detailed media metadata")
    public ResponseEntity<Map<String, Object>> getMediaMetadata(@PathVariable Long mediaId) {
        try {
            Optional<MediaFile> mediaOpt = mediaStorageService.getMedia(mediaId);
            if (mediaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            MediaFile mediaFile = mediaOpt.get();
            Map<String, Object> metadata = Map.of(
                    "id", mediaFile.getId(),
                    "filename", mediaFile.getOriginalFilename(),
                    "mediaType", mediaFile.getMediaCategory().name(),
                    "fileSize", mediaFile.getFileSize(),
                    "mimeType", mediaFile.getMimeType(),
                    "width", mediaFile.getWidth() != null ? mediaFile.getWidth() : 0,
                    "height", mediaFile.getHeight() != null ? mediaFile.getHeight() : 0,
                    "durationSeconds", mediaFile.getDurationSeconds() != null ? mediaFile.getDurationSeconds() : 0,
                    "bitrate", mediaFile.getBitrate() != null ? mediaFile.getBitrate() : 0,
                    "processingStatus", mediaFile.getProcessingStatus().name(),
                    "createdAt", mediaFile.getCreatedAt()
            );

            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media file")
    public ResponseEntity<Map<String, Object>> deleteMedia(
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Add authorization check here
            mediaStorageService.deleteMedia(mediaId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Media deleted successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete media: " + e.getMessage()
            ));
        }
    }

    private Long getUserIdFromDetails(UserDetails userDetails) {
        // Implement based on your UserDetails implementation
        return 1L; // Replace with actual user ID extraction
    }
}