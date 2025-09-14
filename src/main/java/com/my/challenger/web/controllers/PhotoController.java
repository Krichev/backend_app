package com.my.challenger.web.controllers;

import com.my.challenger.entity.Photo;
import com.my.challenger.entity.enums.PhotoType;
import com.my.challenger.service.impl.PhotoMetricsService;
import com.my.challenger.service.impl.PhotoStorageService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Simple photo controller with optional metrics
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final PhotoStorageService photoStorageService;
    
    // Optional - will be null if metrics are disabled
    @Autowired(required = false)
    private PhotoMetricsService metricsService;

    /**
     * Upload avatar photo
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        
        try {
            Photo photo = photoStorageService.savePhoto(file, PhotoType.AVATAR, userId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Avatar uploaded successfully",
                "photoId", photo.getId(),
                "photoUrl", photoStorageService.getPhotoUrl(photo)
            ));
        } catch (IOException e) {
            log.error("Avatar upload failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload challenge cover photo
     */
    @PostMapping("/challenge-cover")
    public ResponseEntity<?> uploadChallengeCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam("challengeId") Long challengeId,
            @RequestParam("userId") Long userId) {
        
        try {
            Photo photo = photoStorageService.savePhoto(
                file, PhotoType.CHALLENGE_COVER, challengeId, userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Challenge cover uploaded successfully",
                "photoId", photo.getId(),
                "photoUrl", photoStorageService.getPhotoUrl(photo)
            ));
        } catch (IOException e) {
            log.error("Challenge cover upload failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete photo
     */
    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long photoId) {
        try {
            photoStorageService.deletePhoto(photoId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Photo deleted successfully"
            ));
        } catch (IOException e) {
            log.error("Photo deletion failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Deletion failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Get metrics summary (only if metrics are enabled)
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        if (metricsService == null) {
            return ResponseEntity.ok(Map.of(
                "enabled", false,
                "message", "Metrics are disabled"
            ));
        }
        
        PhotoMetricsService.MetricsSummary summary = metricsService.getSummary();
        return ResponseEntity.ok(Map.of(
            "enabled", metricsService.isEnabled(),
            "summary", summary,
            "details", Map.of(
                "totalUploads", summary.totalUploads,
                "successfulUploads", summary.successfulUploads,
                "failedUploads", summary.failedUploads,
                "totalDeletes", summary.totalDeletes,
                "storageUsed", summary.getStorageMB(),
                "averageUploadTime", String.format("%.1f ms", summary.avgUploadTimeMs),
                "successRate", String.format("%.1f%%", summary.successRatePercent)
            )
        ));
    }

    /**
     * Get metrics status
     */
    @GetMapping("/metrics/status")
    public ResponseEntity<?> getMetricsStatus() {
        boolean enabled = metricsService != null && metricsService.isEnabled();
        return ResponseEntity.ok(Map.of(
            "metricsEnabled", enabled,
            "metricsService", metricsService != null ? "available" : "disabled"
        ));
    }
}