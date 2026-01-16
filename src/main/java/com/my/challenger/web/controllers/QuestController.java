package com.my.challenger.web.controllers;

import com.my.challenger.dto.quest.QuestAudioConfigDTO;
import com.my.challenger.dto.quest.QuestAudioResponseDTO;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.exception.InvalidAudioSegmentException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.MediaService;
import com.my.challenger.service.QuestAudioService;
import com.my.challenger.service.QuestService;
import com.my.challenger.service.impl.MinioMediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quest Management", description = "Manage quests including audio configuration and lifecycle")
public class QuestController {

    private final QuestAudioService questAudioService;
    private final QuestService questService;
    private final MediaService mediaService;
    private final MinioMediaStorageService storageService;

    @DeleteMapping("/{questId}")
    @Operation(summary = "Delete quest (soft delete)",
            description = "Mark a quest as inactive. Only the creator can delete their quest.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Quest deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only creator can delete"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteQuest(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = ((UserPrincipal) userDetails).getId();
            log.info("🗑️ Request to delete quest {} by user {}", questId, userId);

            questService.deleteQuest(questId, userId);

            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            log.error("❌ Quest not found: {}", e.getMessage());
            throw e;
        } catch (AccessDeniedException e) {
            log.error("❌ Access denied: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error deleting quest: {}", questId, e);
            throw new RuntimeException("Failed to delete quest: " + e.getMessage());
        }
    }

    @PutMapping("/{questId}/audio-config")
    @Operation(summary = "Update quest audio configuration",
            description = "Configure audio track segment and minimum score requirement for a quest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audio configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid audio segment configuration"),
            @ApiResponse(responseCode = "404", description = "Quest or audio media not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QuestAudioResponseDTO> updateAudioConfig(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId,
            @Parameter(description = "Audio configuration", required = true)
            @Valid @RequestBody QuestAudioConfigDTO config,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("🎵 Request to update audio config for quest {}, user: {}",
                    questId, userDetails.getUsername());

            QuestAudioResponseDTO response = questAudioService.updateAudioConfig(questId, config);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("❌ Resource not found: {}", e.getMessage());
            throw e;
        } catch (InvalidAudioSegmentException e) {
            log.error("❌ Invalid audio segment: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error updating audio config for quest: {}", questId, e);
            throw new RuntimeException("Failed to update audio configuration: " + e.getMessage());
        }
    }

    @GetMapping("/{questId}/audio-config")
    @Operation(summary = "Get quest audio configuration",
            description = "Retrieve audio configuration for a quest including URLs and metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audio configuration retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<QuestAudioResponseDTO> getAudioConfig(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId) {

        try {
            log.info("📖 Request to get audio config for quest {}", questId);

            QuestAudioResponseDTO response = questAudioService.getAudioConfig(questId);

            if (response == null) {
                return ResponseEntity.ok().build(); // No audio configured
            }

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("❌ Quest not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error getting audio config for quest: {}", questId, e);
            throw new RuntimeException("Failed to get audio configuration: " + e.getMessage());
        }
    }

    @DeleteMapping("/{questId}/audio-config")
    @Operation(summary = "Remove quest audio configuration",
            description = "Remove audio track configuration from a quest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Audio configuration removed successfully"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> removeAudioConfig(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("🗑️ Request to remove audio config for quest {}, user: {}",
                    questId, userDetails.getUsername());

            questAudioService.removeAudioConfig(questId);

            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            log.error("❌ Quest not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error removing audio config for quest: {}", questId, e);
            throw new RuntimeException("Failed to remove audio configuration: " + e.getMessage());
        }
    }

    @PostMapping("/{questId}/audio")
    @Operation(summary = "Upload audio file for quest",
            description = "Upload an audio file to be used as a quest track (MP3, WAV, AAC, M4A)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audio uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or file type"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "413", description = "File too large (max 100MB)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadQuestAudio(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId,
            @Parameter(description = "Audio file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("📤 Request to upload audio for quest {}, file: {}, user: {}",
                    questId, file.getOriginalFilename(), userDetails.getUsername());

            // Validate file is audio type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                throw new IllegalArgumentException("File must be an audio file (MP3, WAV, AAC, M4A)");
            }

            // Upload audio file (reuse quiz media upload logic)
            MediaFile mediaFile = mediaService.uploadQuizMedia(file, userDetails.getUsername());

            // Build response
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

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            log.error("❌ Quest not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Unexpected error uploading audio for quest: {}", questId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload audio: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
