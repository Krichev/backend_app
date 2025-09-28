package com.my.challenger.repository;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ChallengeDifficulty;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Challenge entity with fixed recommendation system
 */
@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    /**
     * Find challenges by creator ID
     */
    List<Challenge> findByCreatorId(Long creatorId, Pageable pageable);

    /**
     * Get success rate with additional status breakdown
     */
    @Query("SELECT " +
            "COUNT(c) as total, " +
            "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completed, " +
            "COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as active, " +
            "COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failed, " +
            "CASE " +
            "WHEN COUNT(c) = 0 THEN 0.0 " +
            "ELSE (CAST(COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) AS DOUBLE) / " +
            "CAST(COUNT(c) AS DOUBLE) * 100) " +
            "END as successRate " +
            "FROM Challenge c WHERE c.creator.id = :userId")
    Object[] getDetailedSuccessRateByCreatorId(@Param("userId") Long userId);


    /**
     * Get user's challenge success rate - FIXED: Handles division by zero
     */
    @Query("SELECT " +
            "CASE " +
            "WHEN COUNT(c) = 0 THEN 0.0 " +
            "ELSE (CAST(COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) AS DOUBLE) / " +
            "CAST(COUNT(c) AS DOUBLE) * 100) " +
            "END " +
            "FROM Challenge c WHERE c.creator.id = :userId")
    Double getSuccessRateByCreatorId(@Param("userId") Long userId);

    /**
     * Find challenges with filters
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "(:type IS NULL OR c.type = :type) AND " +
            "(:visibility IS NULL OR (c.isPublic = :visibility)) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:targetGroup IS NULL OR c.group.name = :targetGroup)")
    List<Challenge> findWithFilters(
            @Param("type") ChallengeType type,
            @Param("visibility") Boolean visibility,
            @Param("status") ChallengeStatus status,
            @Param("targetGroup") String targetGroup,
            Pageable pageable);

    /**
     * Find challenges by participant ID
     */
    @Query("SELECT DISTINCT c FROM Challenge c JOIN c.progress cp WHERE cp.user.id = :participantId")
    List<Challenge> findChallengesByParticipantId(@Param("participantId") Long participantId, Pageable pageable);

    @Query("SELECT c FROM Challenge c WHERE " +
            "c.creator.id = :userId OR EXISTS (SELECT cp FROM ChallengeProgress cp " +
            "WHERE cp.challenge.id = c.id AND cp.user.id = :userId)")
    List<Challenge> findChallengesByUserIdAsCreatorOrParticipant(@Param("userId") Long userId, Pageable pageable);

    /**
     * Search challenges by keyword in title, description
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Challenge> searchByKeyword(@Param("query") String query);

    /**
     * Count challenges for a specific user (created by them)
     */
    long countByCreatorId(Long creatorId);

    /**
     * Find challenges by difficulty level
     */
    List<Challenge> findByDifficulty(ChallengeDifficulty difficulty);

    /**
     * Find active public challenges by difficulty
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.difficulty = :difficulty " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findActivePublicChallengesByDifficulty(
            @Param("difficulty") ChallengeDifficulty difficulty,
            Pageable pageable);

    /**
     * Find challenges by difficulty range (e.g., EASY to HARD)
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.difficulty >= :minDifficulty " +
            "AND c.difficulty <= :maxDifficulty " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "ORDER BY c.difficulty, c.startDate DESC")
    List<Challenge> findByDifficultyRange(
            @Param("minDifficulty") ChallengeDifficulty minDifficulty,
            @Param("maxDifficulty") ChallengeDifficulty maxDifficulty,
            Pageable pageable);

    /**
     * Find challenges easier than specified difficulty
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.difficulty < :maxDifficulty " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "ORDER BY c.difficulty DESC, c.startDate DESC")
    List<Challenge> findEasierThan(
            @Param("maxDifficulty") ChallengeDifficulty maxDifficulty,
            Pageable pageable);

    /**
     * Find challenges harder than specified difficulty
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.difficulty > :minDifficulty " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "ORDER BY c.difficulty ASC, c.startDate DESC")
    List<Challenge> findHarderThan(
            @Param("minDifficulty") ChallengeDifficulty minDifficulty,
            Pageable pageable);

    /**
     * Count challenges by difficulty
     */
    long countByDifficulty(ChallengeDifficulty difficulty);

    /**
     * Count active challenges by difficulty
     */
    @Query("SELECT COUNT(c) FROM Challenge c WHERE " +
            "c.difficulty = :difficulty AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE")
    long countActiveByDifficulty(@Param("difficulty") ChallengeDifficulty difficulty);

    /**
     * Find user's challenges by difficulty
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.creator.id = :userId " +
            "AND c.difficulty = :difficulty " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findByCreatorIdAndDifficulty(
            @Param("userId") Long userId,
            @Param("difficulty") ChallengeDifficulty difficulty);

    /**
     * Get difficulty distribution statistics
     */
    @Query("SELECT c.difficulty, COUNT(c) as count, " +
            "AVG((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c)) as avgParticipants, " +
            "COUNT(CASE WHEN c.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED THEN 1 END) as completedCount " +
            "FROM Challenge c " +
            "GROUP BY c.difficulty " +
            "ORDER BY c.difficulty")
    List<Object[]> getDifficultyStatistics();

    /**
     * Find trending challenges by difficulty
     */
    @Query("SELECT c, COUNT(cp) as participantCount FROM Challenge c " +
            "LEFT JOIN c.progress cp " +
            "WHERE c.difficulty = :difficulty " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "AND c.startDate >= :sinceDate " +
            "GROUP BY c " +
            "ORDER BY participantCount DESC, c.startDate DESC")
    List<Object[]> findTrendingByDifficulty(
            @Param("difficulty") ChallengeDifficulty difficulty,
            @Param("sinceDate") LocalDateTime sinceDate,
            Pageable pageable);

    /**
     * Search challenges by keyword and difficulty
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.difficulty = :difficulty " +
            "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> searchByKeywordAndDifficulty(
            @Param("query") String query,
            @Param("difficulty") ChallengeDifficulty difficulty,
            Pageable pageable);

    // ========== FIXED RECOMMENDATION SYSTEM ==========

    /**
     * Get user's average difficulty level from completed challenges
     * Returns null if user has no completed challenges
     */
    @Query("SELECT AVG(CASE c.difficulty " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.BEGINNER THEN 1.0 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EASY THEN 2.0 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.MEDIUM THEN 3.0 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.HARD THEN 4.0 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EXPERT THEN 5.0 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EXTREME THEN 6.0 " +
            "ELSE 0.0 " +
            "END) " +
            "FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :userId " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED")
    Optional<Double> getAverageDifficultyForUser(@Param("userId") Long userId);

    /**
     * Get count of user's completed challenges by difficulty
     */
    @Query("SELECT c.difficulty, COUNT(c) " +
            "FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :userId " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED " +
            "GROUP BY c.difficulty " +
            "ORDER BY c.difficulty")
    List<Object[]> getUserCompletedChallengesByDifficulty(@Param("userId") Long userId);

    /**
     * Find recommended challenges based on specific difficulty level
     * This method replaces the complex original query
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "AND c.difficulty = :recommendedDifficulty " +
            "AND NOT EXISTS (SELECT cp FROM c.progress cp WHERE cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findRecommendedByDifficulty(
            @Param("userId") Long userId,
            @Param("recommendedDifficulty") ChallengeDifficulty recommendedDifficulty,
            Pageable pageable);

    /**
     * Find recommended challenges with multiple difficulty levels
     * For cases where we want to recommend challenges from multiple difficulty levels
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "AND c.difficulty IN :recommendedDifficulties " +
            "AND NOT EXISTS (SELECT cp FROM c.progress cp WHERE cp.user.id = :userId) " +
            "ORDER BY " +
            "CASE c.difficulty " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.BEGINNER THEN 1 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EASY THEN 2 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.MEDIUM THEN 3 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.HARD THEN 4 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EXPERT THEN 5 " +
            "WHEN com.my.challenger.entity.enums.ChallengeDifficulty.EXTREME THEN 6 " +
            "END, " +
            "c.startDate DESC")
    List<Challenge> findRecommendedByMultipleDifficulties(
            @Param("userId") Long userId,
            @Param("recommendedDifficulties") List<ChallengeDifficulty> recommendedDifficulties,
            Pageable pageable);

    /**
     * Check if user has any completed challenges
     */
    @Query("SELECT COUNT(c) > 0 " +
            "FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :userId " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED")
    boolean hasUserCompletedAnyChallenge(@Param("userId") Long userId);

    /**
     * Get user's highest completed difficulty level
     */
    @Query("SELECT MAX(c.difficulty) " +
            "FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :userId " +
            "AND c.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED")
    Optional<ChallengeDifficulty> getUserHighestCompletedDifficulty(@Param("userId") Long userId);

    /**
     * Find challenges that need attention (low participation or completion)
     */
    @Query("SELECT c FROM Challenge c " +
            "WHERE c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND ((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c) < :minParticipants " +
            "     OR (SELECT AVG(cp2.completionPercentage) FROM ChallengeProgress cp2 WHERE cp2.challenge = c) < :minCompletion) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesNeedingAttention(
            @Param("minParticipants") Long minParticipants,
            @Param("minCompletion") Double minCompletion);

    /**
     * Find engagement statistics by challenge type
     */
    @Query("SELECT c.type, " +
            "COUNT(c) as totalChallenges, " +
            "AVG((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c)) as avgParticipants, " +
            "AVG((SELECT AVG(cp2.completionPercentage) FROM ChallengeProgress cp2 WHERE cp2.challenge = c)) as avgCompletion " +
            "FROM Challenge c " +
            "WHERE c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "GROUP BY c.type")
    List<Object[]> findEngagementByType();
}