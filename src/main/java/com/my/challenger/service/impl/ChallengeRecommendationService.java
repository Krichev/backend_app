package com.my.challenger.service.impl;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ChallengeDifficulty;
import com.my.challenger.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for providing intelligent challenge recommendations based on user history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeRecommendationService {

    private final ChallengeRepository challengeRepository;

    /**
     * Find recommended challenges based on user's completed difficulty levels
     * This replaces the complex repository method with clean business logic
     */
    public List<Challenge> findRecommendedByUserHistory(Long userId, Pageable pageable) {
        log.debug("Finding recommended challenges for user: {}", userId);

        // Check if user has completed any challenges
        if (!challengeRepository.hasUserCompletedAnyChallenge(userId)) {
            log.debug("User {} has no completed challenges, recommending BEGINNER level", userId);
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.BEGINNER, pageable);
        }

        // Get user's average difficulty
        Optional<Double> avgDifficultyOpt = challengeRepository.getAverageDifficultyForUser(userId);

        if (avgDifficultyOpt.isEmpty()) {
            log.debug("No average difficulty found for user {}, defaulting to BEGINNER", userId);
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.BEGINNER, pageable);
        }

        double avgDifficulty = avgDifficultyOpt.get();
        log.debug("User {} average difficulty: {}", userId, avgDifficulty);

        // Determine recommended difficulty based on average
        ChallengeDifficulty recommendedDifficulty = determineRecommendedDifficulty(avgDifficulty);

        log.debug("Recommended difficulty for user {}: {}", userId, recommendedDifficulty);

        // Get challenges at recommended difficulty
        List<Challenge> recommendations = challengeRepository.findRecommendedByDifficulty(
                userId, recommendedDifficulty, pageable);

        // If no challenges found at recommended level, try adjacent levels
        if (recommendations.isEmpty()) {
            log.debug("No challenges found at {} level for user {}, trying adjacent levels",
                    recommendedDifficulty, userId);
            recommendations = findAlternativeRecommendations(userId, recommendedDifficulty, pageable);
        }

        log.debug("Found {} recommended challenges for user {}", recommendations.size(), userId);
        return recommendations;
    }

    /**
     * Get adaptive recommendations that include multiple difficulty levels
     * Based on user's completion history and performance
     */
    public List<Challenge> findAdaptiveRecommendations(Long userId, Pageable pageable) {
        log.debug("Finding adaptive recommendations for user: {}", userId);

        if (!challengeRepository.hasUserCompletedAnyChallenge(userId)) {
            // New user - recommend beginner and easy levels
            List<ChallengeDifficulty> difficulties = Arrays.asList(
                    ChallengeDifficulty.BEGINNER, ChallengeDifficulty.EASY);
            return challengeRepository.findRecommendedByMultipleDifficulties(userId, difficulties, pageable);
        }

        Optional<Double> avgDifficultyOpt = challengeRepository.getAverageDifficultyForUser(userId);
        if (avgDifficultyOpt.isEmpty()) {
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.BEGINNER, pageable);
        }

        double avgDifficulty = avgDifficultyOpt.get();
        List<ChallengeDifficulty> recommendedDifficulties = getAdaptiveDifficulties(avgDifficulty);

        return challengeRepository.findRecommendedByMultipleDifficulties(userId, recommendedDifficulties, pageable);
    }

    /**
     * Get progressive recommendations that gradually increase difficulty
     */
    public List<Challenge> findProgressiveRecommendations(Long userId, Pageable pageable) {
        log.debug("Finding progressive recommendations for user: {}", userId);

        Optional<ChallengeDifficulty> highestCompleted = challengeRepository.getUserHighestCompletedDifficulty(userId);

        if (highestCompleted.isEmpty()) {
            // Start with beginner
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.BEGINNER, pageable);
        }

        ChallengeDifficulty nextDifficulty = getNextProgressiveDifficulty(highestCompleted.get());
        log.debug("User {} highest completed: {}, next progressive: {}",
                userId, highestCompleted.get(), nextDifficulty);

        return challengeRepository.findRecommendedByDifficulty(userId, nextDifficulty, pageable);
    }

    /**
     * Determine recommended difficulty based on average completed difficulty
     */
    private ChallengeDifficulty determineRecommendedDifficulty(double avgDifficulty) {
        if (avgDifficulty < 1.5) {
            return ChallengeDifficulty.BEGINNER;
        } else if (avgDifficulty < 2.5) {
            return ChallengeDifficulty.EASY;
        } else if (avgDifficulty < 3.5) {
            return ChallengeDifficulty.MEDIUM;
        } else if (avgDifficulty < 4.5) {
            return ChallengeDifficulty.HARD;
        } else if (avgDifficulty < 5.5) {
            return ChallengeDifficulty.EXPERT;
        } else {
            return ChallengeDifficulty.EXTREME;
        }
    }

    /**
     * Get multiple difficulty levels for adaptive recommendations
     */
    private List<ChallengeDifficulty> getAdaptiveDifficulties(double avgDifficulty) {
        List<ChallengeDifficulty> difficulties = new ArrayList<>();

        ChallengeDifficulty primary = determineRecommendedDifficulty(avgDifficulty);
        difficulties.add(primary);

        // Add adjacent difficulties for variety
        ChallengeDifficulty[] allDifficulties = ChallengeDifficulty.values();
        int primaryIndex = primary.ordinal();

        // Add one level below if possible
        if (primaryIndex > 0) {
            difficulties.add(allDifficulties[primaryIndex - 1]);
        }

        // Add one level above if possible and user is progressing well
        if (primaryIndex < allDifficulties.length - 1 && avgDifficulty > primary.getLevel() - 0.3) {
            difficulties.add(allDifficulties[primaryIndex + 1]);
        }

        return difficulties;
    }

    /**
     * Get next difficulty level for progressive recommendations
     */
    private ChallengeDifficulty getNextProgressiveDifficulty(ChallengeDifficulty highest) {
        ChallengeDifficulty[] allDifficulties = ChallengeDifficulty.values();
        int currentIndex = highest.ordinal();

        // Move to next level if not at maximum
        if (currentIndex < allDifficulties.length - 1) {
            return allDifficulties[currentIndex + 1];
        }

        // Stay at highest level if already at maximum
        return highest;
    }

    /**
     * Find alternative recommendations when none found at primary level
     */
    private List<Challenge> findAlternativeRecommendations(Long userId, ChallengeDifficulty primaryDifficulty, Pageable pageable) {
        ChallengeDifficulty[] allDifficulties = ChallengeDifficulty.values();
        int primaryIndex = primaryDifficulty.ordinal();

        // Try one level below first
        if (primaryIndex > 0) {
            List<Challenge> alternatives = challengeRepository.findRecommendedByDifficulty(
                    userId, allDifficulties[primaryIndex - 1], pageable);
            if (!alternatives.isEmpty()) {
                return alternatives;
            }
        }

        // Try one level above
        if (primaryIndex < allDifficulties.length - 1) {
            List<Challenge> alternatives = challengeRepository.findRecommendedByDifficulty(
                    userId, allDifficulties[primaryIndex + 1], pageable);
            if (!alternatives.isEmpty()) {
                return alternatives;
            }
        }

        // Fallback to any available challenges (excluding user's current challenges)
        return challengeRepository.findRecommendedByMultipleDifficulties(
                userId, Arrays.asList(allDifficulties), pageable);
    }

    /**
     * Get user's completion statistics for analysis
     */
    public UserDifficultyStats getUserDifficultyStats(Long userId) {
        List<Object[]> stats = challengeRepository.getUserCompletedChallengesByDifficulty(userId);
        Optional<Double> avgDifficulty = challengeRepository.getAverageDifficultyForUser(userId);
        Optional<ChallengeDifficulty> highest = challengeRepository.getUserHighestCompletedDifficulty(userId);

        return UserDifficultyStats.builder()
                .userId(userId)
                .completedByDifficulty(stats)
                .averageDifficulty(avgDifficulty.orElse(0.0))
                .highestCompleted(highest.orElse(null))
                .hasCompletedChallenges(challengeRepository.hasUserCompletedAnyChallenge(userId))
                .build();
    }

    /**
     * DTO for user difficulty statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class UserDifficultyStats {
        private Long userId;
        private List<Object[]> completedByDifficulty;
        private Double averageDifficulty;
        private ChallengeDifficulty highestCompleted;
        private Boolean hasCompletedChallenges;
    }
}