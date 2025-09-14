package com.my.challenger.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Simple, practical metrics service for photo operations
 * Tracks only the essential metrics that matter for photo management
 */
@Service
@ConditionalOnProperty(name = "app.metrics.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class PhotoMetricsService {

    private final MeterRegistry meterRegistry;

    // Core metrics - only track what matters
    private Counter uploadsTotal;
    private Counter uploadsSuccess;
    private Counter uploadsFailed;
    private Counter deletesTotal;
    private Timer uploadDuration;
    private Counter storageBytes;

    public PhotoMetricsService(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void initMetrics() {
        if (meterRegistry == null) {
            log.info("MeterRegistry not available - metrics disabled");
            return;
        }

        try {
            // Track total upload attempts
            uploadsTotal = Counter.builder("photos.uploads.total")
                    .description("Total photo upload attempts")
                    .register(meterRegistry);

            // Track successful uploads
            uploadsSuccess = Counter.builder("photos.uploads.success")
                    .description("Successful photo uploads")
                    .register(meterRegistry);

            // Track failed uploads
            uploadsFailed = Counter.builder("photos.uploads.failed")
                    .description("Failed photo uploads")
                    .register(meterRegistry);

            // Track upload time
            uploadDuration = Timer.builder("photos.upload.duration")
                    .description("Photo upload duration")
                    .register(meterRegistry);

            // Track deletions
            deletesTotal = Counter.builder("photos.deletes.total")
                    .description("Total photo deletions")
                    .register(meterRegistry);

            // Track storage usage
            storageBytes = Counter.builder("photos.storage.bytes")
                    .description("Total storage used by photos")
                    .register(meterRegistry);

            log.info("Photo metrics initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize photo metrics: {}", e.getMessage());
        }
    }

    /**
     * Record a successful photo upload
     */
    public void recordSuccessfulUpload(long fileSizeBytes, long durationMs) {
        safeIncrement(uploadsTotal);
        safeIncrement(uploadsSuccess);
        safeRecord(uploadDuration, durationMs, TimeUnit.MILLISECONDS);
        safeIncrement(storageBytes, fileSizeBytes);

        log.debug("Recorded successful upload: {} bytes in {} ms", fileSizeBytes, durationMs);
    }

    /**
     * Record a failed photo upload
     */
    public void recordFailedUpload(String reason) {
        safeIncrement(uploadsTotal);
        safeIncrement(uploadsFailed);

        log.debug("Recorded failed upload: {}", reason);
    }

    /**
     * Record a photo deletion
     */
    public void recordDeletion(long fileSizeBytes) {
        safeIncrement(deletesTotal);
        safeIncrement(storageBytes, -fileSizeBytes); // Subtract deleted bytes

        log.debug("Recorded deletion: {} bytes", fileSizeBytes);
    }

    /**
     * Get current metrics summary
     */
    public MetricsSummary getSummary() {
        if (!isEnabled()) {
            return MetricsSummary.disabled();
        }

        try {
            return new MetricsSummary(
                    Math.round(uploadsTotal.count()),
                    Math.round(uploadsSuccess.count()),
                    Math.round(uploadsFailed.count()),
                    Math.round(deletesTotal.count()),
                    Math.round(storageBytes.count()),
                    uploadDuration.mean(TimeUnit.MILLISECONDS),
                    calculateSuccessRate()
            );
        } catch (Exception e) {
            log.warn("Error getting metrics summary: {}", e.getMessage());
            return MetricsSummary.error();
        }
    }

    /**
     * Check if metrics are enabled and working
     */
    public boolean isEnabled() {
        return meterRegistry != null && uploadsTotal != null;
    }

    // Private helper methods

    private void safeIncrement(Counter counter) {
        if (counter != null) {
            try {
                counter.increment();
            } catch (Exception e) {
                log.debug("Error incrementing counter: {}", e.getMessage());
            }
        }
    }

    private void safeIncrement(Counter counter, double amount) {
        if (counter != null) {
            try {
                counter.increment(amount);
            } catch (Exception e) {
                log.debug("Error incrementing counter by amount: {}", e.getMessage());
            }
        }
    }

    private void safeRecord(Timer timer, long value, TimeUnit unit) {
        if (timer != null) {
            try {
                timer.record(value, unit);
            } catch (Exception e) {
                log.debug("Error recording timer: {}", e.getMessage());
            }
        }
    }

    private double calculateSuccessRate() {
        if (uploadsTotal == null || uploadsTotal.count() == 0) {
            return 100.0;
        }
        return (uploadsSuccess.count() / uploadsTotal.count()) * 100.0;
    }

    /**
     * Metrics summary data class
     */
    public static class MetricsSummary {
        public final long totalUploads;
        public final long successfulUploads;
        public final long failedUploads;
        public final long totalDeletes;
        public final long storageBytes;
        public final double avgUploadTimeMs;
        public final double successRatePercent;
        public final boolean enabled;
        public final String status;

        private MetricsSummary(long totalUploads, long successfulUploads, long failedUploads,
                               long totalDeletes, long storageBytes, double avgUploadTimeMs,
                               double successRatePercent) {
            this.totalUploads = totalUploads;
            this.successfulUploads = successfulUploads;
            this.failedUploads = failedUploads;
            this.totalDeletes = totalDeletes;
            this.storageBytes = storageBytes;
            this.avgUploadTimeMs = avgUploadTimeMs;
            this.successRatePercent = successRatePercent;
            this.enabled = true;
            this.status = "active";
        }

        public static MetricsSummary disabled() {
            return new MetricsSummary(0, 0, 0, 0, 0, 0, 0) {
                public final boolean enabled = false;
                public final String status = "disabled";
            };
        }

        public static MetricsSummary error() {
            return new MetricsSummary(0, 0, 0, 0, 0, 0, 0) {
                public final boolean enabled = true;
                public final String status = "error";
            };
        }

        public String getStorageMB() {
            return String.format("%.2f MB", storageBytes / (1024.0 * 1024.0));
        }

        public String getStorageGB() {
            return String.format("%.2f GB", storageBytes / (1024.0 * 1024.0 * 1024.0));
        }

        @Override
        public String toString() {
            if (!enabled) return "Metrics disabled";
            if ("error".equals(status)) return "Metrics error";

            return String.format(
                    "Photos: %d uploads (%d success, %d failed), %d deletes, %s storage, %.1fms avg, %.1f%% success",
                    totalUploads, successfulUploads, failedUploads, totalDeletes,
                    getStorageMB(), avgUploadTimeMs, successRatePercent
            );
        }
    }
}