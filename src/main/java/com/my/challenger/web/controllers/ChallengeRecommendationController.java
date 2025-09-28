package com.my.challenger.web.controllers;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.service.impl.ChallengeRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller methods for challenge recommendations
 * Add these methods to your existing ChallengeController
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class ChallengeRecommendationController {

    private final ChallengeRecommendationService recommendationService;

    /**
     * Get recommended challenges based on user's history
     * This replaces your original findRecommendedByUserHistory method
     */
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<Challenge>> getRecommendedChallenges(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Getting recommended challenges for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Challenge> recommendations = recommendationService.findRecommendedByUserHistory(userId, pageable);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get adaptive recommendations with multiple difficulty levels
     */
    @GetMapping("/recommendations/{userId}/adaptive")
    public ResponseEntity<List<Challenge>> getAdaptiveRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        log.info("Getting adaptive recommendations for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Challenge> recommendations = recommendationService.findAdaptiveRecommendations(userId, pageable);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get progressive recommendations for skill building
     */
    @GetMapping("/recommendations/{userId}/progressive")
    public ResponseEntity<List<Challenge>> getProgressiveRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Getting progressive recommendations for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        List<Challenge> recommendations = recommendationService.findProgressiveRecommendations(userId, pageable);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get user's difficulty statistics
     */
    @GetMapping("/recommendations/{userId}/stats")
    public ResponseEntity<ChallengeRecommendationService.UserDifficultyStats> getUserDifficultyStats(
            @PathVariable Long userId) {
        
        log.info("Getting difficulty stats for user: {}", userId);
        
        ChallengeRecommendationService.UserDifficultyStats stats = 
                recommendationService.getUserDifficultyStats(userId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Example of how to use the recommendation service in other parts of your application
     */
    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<DashboardData> getUserDashboard(@PathVariable Long userId) {
        log.info("Loading dashboard for user: {}", userId);
        
        // Get different types of recommendations for dashboard
        Pageable smallPage = PageRequest.of(0, 5);
        
        List<Challenge> quickRecommendations = recommendationService.findRecommendedByUserHistory(userId, smallPage);
        List<Challenge> progressiveRecommendations = recommendationService.findProgressiveRecommendations(userId, smallPage);
        ChallengeRecommendationService.UserDifficultyStats stats = recommendationService.getUserDifficultyStats(userId);
        
        DashboardData dashboard = DashboardData.builder()
                .userId(userId)
                .recommendedChallenges(quickRecommendations)
                .progressiveChallenges(progressiveRecommendations)
                .userStats(stats)
                .build();
        
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Dashboard data DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class DashboardData {
        private Long userId;
        private List<Challenge> recommendedChallenges;
        private List<Challenge> progressiveChallenges;
        private ChallengeRecommendationService.UserDifficultyStats userStats;
    }
}