package com.my.challenger.service.impl;

import com.my.challenger.service.impl.PhotoMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for photo metrics endpoints
 */
@RestController
@RequestMapping("/metrics/photos")
@RequiredArgsConstructor
@ConditionalOnBean(PhotoMetricsService.class)
public class PhotoMetricsController {

    private final PhotoMetricsService photoMetricsService;

//    /**
//     * Get photo metrics snapshot
//     */
//    @GetMapping("/snapshot")
//    public ResponseEntity<PhotoMetricsService.MetricsSnapshot> getMetricsSnapshot() {
//        return ResponseEntity.ok(photoMetricsService.getMetricsSnapshot());
//    }
//
//    /**
//     * Check if metrics are enabled
//     */
//    @GetMapping("/status")
//    public ResponseEntity<Map<String, Object>> getMetricsStatus() {
//        boolean enabled = photoMetricsService.isMetricsEnabled();
//        Map<String, Object> status = Map.of(
//                "enabled", enabled,
//                "service", "PhotoMetricsService"
//        );
//        return ResponseEntity.ok(status);
//    }
//
//    /**
//     * Get metrics health check
//     */
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> getHealth() {
//        boolean enabled = photoMetricsService.isMetricsEnabled();
//        String status = enabled ? "UP" : "DOWN";
//        String description = enabled ? "Photo metrics are working" : "Photo metrics are disabled";
//
//        Map<String, String> health = Map.of(
//                "status", status,
//                "description", description
//        );
//
//        return ResponseEntity.ok(health);
//    }
}