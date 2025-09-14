package com.my.challenger.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-operation metrics service that does nothing
 * Used as fallback when metrics are disabled or MeterRegistry is not available
 */
@Service
@ConditionalOnMissingBean(PhotoMetricsService.class)
@Slf4j
public class NoOpPhotoMetricsService {
    
    public NoOpPhotoMetricsService() {
        log.info("Using NoOp metrics service - metrics collection disabled");
    }
    
    public void recordUpload(long bytes, long durationMillis) {
        // No-op
    }
    
    public void recordDeletion(long bytes) {
        // No-op
    }
    
    public void recordError(String operation) {
        // No-op
    }
    
    public void recordError(String operation, String errorType) {
        // No-op
    }
    
    public void recordProcessingTime(long durationMillis, String processingType) {
        // No-op
    }
    
    public void recordPhotoSize(long bytes, String photoType) {
        // No-op
    }
    
    public void recordPhotoDimensions(int width, int height, String photoType) {
        // No-op
    }
    
    public boolean isMetricsEnabled() {
        return false;
    }
    
    public MetricsSnapshot getMetricsSnapshot() {
        return new MetricsSnapshot(0L, 0L, 0L, 0L, 0.0);
    }
    
    public record MetricsSnapshot(
            long totalUploads,
            long totalDeletes, 
            long totalErrors,
            long totalStorageBytes,
            double averageUploadTimeMs
    ) {}
}