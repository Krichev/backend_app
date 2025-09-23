package com.my.challenger.service.impl;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ChallengeDifficulty;
import com.my.challenger.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeRecommendationService {

    private final ChallengeRepository challengeRepository;

    /**
     * Find recommended challenges based on user's completed difficulty levels
     * This method implements the complex logic that was previously in the repository
     */
    public List<Challenge> findRecommendedByUserHistory(Long userId, Pageable pageable) {
        // Get user's average difficulty from completed challenges
        Double averageDifficulty = challengeRepository.getAverageDifficultyForUser(userId);
        
        // Determine recommended difficulty based on user's history
        ChallengeDifficulty recommendedDifficulty = determineRecommendedDifficulty(averageDifficulty);
        
        log.debug("User {} average difficulty: {}, recommended: {}", 
                 userId, averageDifficulty, recommendedDifficulty);
        
        // Find challenges with the recommended difficulty
        return challengeRepository.findRecommendedByDifficulty(userId, recommendedDifficulty, pageable);
    }

    /**
     * Determine recommended difficulty based on user's average performance
     */
    private ChallengeDifficulty determineRecommendedDifficulty(Double averageDifficulty) {
        if (averageDifficulty == null || averageDifficulty == 0.0) {
            // New user with no completed challenges - start with EASY
            return ChallengeDifficulty.EASY;
        }
        
        // Map average difficulty to recommended next level
        if (averageDifficulty < 2.0) {
            return ChallengeDifficulty.EASY;           // Still learning basics
        } else if (averageDifficulty < 3.0) {
            return ChallengeDifficulty.MEDIUM;         // Ready for moderate challenges
        } else if (averageDifficulty < 4.0) {
            return ChallengeDifficulty.HARD;           // Can handle difficult tasks
        } else if (averageDifficulty < 5.0) {
            return ChallengeDifficulty.EXPERT;         // Advanced user
        } else {
            return ChallengeDifficulty.EXTREME;        // Elite performer
        }
    }

    /**
     * Get personalized difficulty recommendation with explanation
     */
    public DifficultyRecommendation getPersonalizedRecommendation(Long userId) {
        Double averageDifficulty = challengeRepository.getAverageDifficultyForUser(userId);
        ChallengeDifficulty recommendedDifficulty = determineRecommendedDifficulty(averageDifficulty);
        
        String explanation = generateRecommendationExplanation(averageDifficulty, recommendedDifficulty);
        
        return new DifficultyRecommendation(
            recommendedDifficulty,
            averageDifficulty,
            explanation
        );
    }

    /**
     * Generate human-readable explanation for the recommendation
     */
    private String generateRecommendationExplanation(Double averageDifficulty, ChallengeDifficulty recommended) {
        if (averageDifficulty == null || averageDifficulty == 0.0) {
            return "Since you're new to challenges, we recommend starting with " + 
                   recommended.getDisplayName().toLowerCase() + " level challenges.";
        }
        
        String avgLevel = ChallengeDifficulty.fromLevel((int) Math.round(averageDifficulty)).getDisplayName();
        
        return String.format(
            "Based on your average performance at %s level (%.1f), we recommend trying %s challenges next.",
            avgLevel.toLowerCase(), averageDifficulty, recommended.getDisplayName().toLowerCase()
        );
    }

    /**
     * Find challenges that are one level easier than user's average (for confidence building)
     */
    public List<Challenge> findEasierChallenges(Long userId, Pageable pageable) {
        Double averageDifficulty = challengeRepository.getAverageDifficultyForUser(userId);
        
        if (averageDifficulty == null || averageDifficulty <= 1.0) {
            // Already at minimum, stay at BEGINNER/EASY
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.BEGINNER, pageable);
        }
        
        // One level easier
        ChallengeDifficulty easierDifficulty = ChallengeDifficulty.fromLevel(
            Math.max(1, (int) Math.round(averageDifficulty) - 1)
        );
        
        return challengeRepository.findRecommendedByDifficulty(userId, easierDifficulty, pageable);
    }

    /**
     * Find challenges that are one level harder than user's average (for growth)
     */
    public List<Challenge> findHarderChallenges(Long userId, Pageable pageable) {
        Double averageDifficulty = challengeRepository.getAverageDifficultyForUser(userId);
        
        if (averageDifficulty == null) {
            // New user - recommend EASY instead of MEDIUM
            return challengeRepository.findRecommendedByDifficulty(userId, ChallengeDifficulty.EASY, pageable);
        }
        
        // One level harder, but cap at EXTREME
        ChallengeDifficulty harderDifficulty = ChallengeDifficulty.fromLevel(
            Math.min(6, (int) Math.round(averageDifficulty) + 1)
        );
        
        return challengeRepository.findRecommendedByDifficulty(userId, harderDifficulty, pageable);
    }

    /**
     * Data class for difficulty recommendations
     */
    public static class DifficultyRecommendation {
        private final ChallengeDifficulty recommendedDifficulty;
        private final Double currentAverageLevel;
        private final String explanation;

        public DifficultyRecommendation(ChallengeDifficulty recommendedDifficulty, 
                                      Double currentAverageLevel, 
                                      String explanation) {
            this.recommendedDifficulty = recommendedDifficulty;
            this.currentAverageLevel = currentAverageLevel;
            this.explanation = explanation;
        }

        public ChallengeDifficulty getRecommendedDifficulty() {
            return recommendedDifficulty;
        }

        public Double getCurrentAverageLevel() {
            return currentAverageLevel;
        }

        public String getExplanation() {
            return explanation;
        }

        @Override
        public String toString() {
            return String.format("DifficultyRecommendation{recommended=%s, currentAverage=%.1f, explanation='%s'}", 
                               recommendedDifficulty, currentAverageLevel, explanation);
        }
    }
}