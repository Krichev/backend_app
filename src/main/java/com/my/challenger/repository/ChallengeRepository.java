// Additional repository for challenge-related statistics
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

/**
 * Repository for Challenge entity
 * Fixed to work with the new progress system instead of old participants relationship
 */
@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    /**
     * Find challenges by creator ID
     */
    List<Challenge> findByCreatorId(Long creatorId, Pageable pageable);

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
     * Count active challenges for a specific user
     */
    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.creator.id = :userId AND c.status = 'ACTIVE'")
    long countActiveByCreatorId(@Param("userId") Long userId);

    /**
     * Find public challenges for explore page
     */
    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true AND c.status = 'ACTIVE' " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findPublicChallenges(Pageable pageable);

    /**
     * Find trending challenges (most participants)
     */
    @Query("SELECT c, COUNT(cp) AS participantCount FROM Challenge c " +
            "JOIN ChallengeProgress cp ON c.id = cp.challenge.id " +
            "WHERE c.isPublic = true AND c.status = 'ACTIVE' " +
            "GROUP BY c.id ORDER BY participantCount DESC")
    List<Challenge> findTrendingChallenges(Pageable pageable);

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
     * Alternative method: Get detailed success rate stats
     */
    @Query("SELECT " +
            "COUNT(c) as totalChallenges, " +
            "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedChallenges " +
            "FROM Challenge c WHERE c.creator.id = :userId")
    Object[] getSuccessRateStatsById(@Param("userId") Long userId);

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
     * Find challenges by creator ID (without pagination)
     */
    List<Challenge> findByCreatorId(Long creatorId);


    /**
     * FIXED: Find challenges by participant ID (without pagination)
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :participantId " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesByParticipantId(@Param("participantId") Long participantId);


    /**
     * FIXED: Find challenges where user is creator OR participant (without pagination)
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "WHERE c.creator.id = :userId " +
            "OR EXISTS (SELECT cp FROM ChallengeProgress cp " +
            "           WHERE cp.challenge.id = c.id AND cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesByUserIdAsCreatorOrParticipant(@Param("userId") Long userId);

    /**
     * Search challenges by keyword in title, description
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> searchByKeyword(@Param("query") String query, Pageable pageable);


    /**
     * Find active public challenges
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.isPublic = true AND c.status = 'ACTIVE' " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findActivePublicChallenges(Pageable pageable);

    /**
     * Find challenges ending soon (within specified hours)
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.status = 'ACTIVE' AND " +
            "c.endDate IS NOT NULL AND " +
            "c.endDate BETWEEN :now AND :endTime " +
            "ORDER BY c.endDate ASC")
    List<Challenge> findChallengesEndingSoon(
            @Param("now") LocalDateTime now,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find challenges by type
     */
    List<Challenge> findByType(ChallengeType type, Pageable pageable);

    /**
     * Find challenges by status
     */
    List<Challenge> findByStatus(ChallengeStatus status, Pageable pageable);


    /**
     * Find private challenges
     */
    @Query("SELECT c FROM Challenge c WHERE c.isPublic = false ORDER BY c.startDate DESC")
    List<Challenge> findPrivateChallenges(Pageable pageable);

    // ========================================================================
    // NEW METHODS - Enabled by Progress System
    // ========================================================================

    /**
     * Find most popular challenges (highest participant count)
     */
    @Query("SELECT c, COUNT(cp) as participantCount FROM Challenge c " +
            "LEFT JOIN c.progress cp " +
            "WHERE c.status = 'ACTIVE' AND c.isPublic = true " +
            "GROUP BY c " +
            "ORDER BY participantCount DESC")
    List<Object[]> findMostPopularChallenges(Pageable pageable);

    /**
     * Find challenges with high completion rates
     */
    @Query("SELECT c, AVG(cp.completionPercentage) as avgCompletion FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE c.status = 'ACTIVE' " +
            "GROUP BY c " +
            "HAVING AVG(cp.completionPercentage) >= :minCompletionRate " +
            "ORDER BY avgCompletion DESC")
    List<Object[]> findHighCompletionRateChallenges(
            @Param("minCompletionRate") Double minCompletionRate,
            Pageable pageable);

    /**
     * Find challenges with low engagement (completion < threshold)
     */
    @Query("SELECT c, AVG(cp.completionPercentage) as avgCompletion FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE c.status = 'ACTIVE' " +
            "GROUP BY c " +
            "HAVING AVG(cp.completionPercentage) < :maxCompletionRate " +
            "ORDER BY avgCompletion ASC")
    List<Object[]> findLowEngagementChallenges(
            @Param("maxCompletionRate") Double maxCompletionRate,
            Pageable pageable);

    /**
     * Find challenges a user might be interested in (similar to ones they've joined)
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "WHERE c.type IN (SELECT DISTINCT c2.type FROM Challenge c2 " +
            "                 JOIN c2.progress cp2 WHERE cp2.user.id = :userId) " +
            "AND c.status = 'ACTIVE' " +
            "AND c.isPublic = true " +
            "AND NOT EXISTS (SELECT cp FROM c.progress cp WHERE cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findSimilarChallenges(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find trending challenges (recently created with good early engagement)
     */
    @Query("SELECT c, COUNT(cp) as participantCount FROM Challenge c " +
            "LEFT JOIN c.progress cp " +
            "WHERE c.status = 'ACTIVE' " +
            "AND c.isPublic = true " +
            "AND c.startDate >= :sinceDate " +
            "GROUP BY c " +
            "ORDER BY participantCount DESC, c.startDate DESC")
    List<Object[]> findTrendingChallenges(
            @Param("sinceDate") LocalDateTime sinceDate,
            Pageable pageable);

    /**
     * Count active challenges
     */
    long countByStatus(ChallengeStatus status);

    /**
     * Count public challenges
     */
    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.isPublic = true")
    long countPublicChallenges();

    /**
     * Find challenges by date range
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.startDate >= :startDate " +
            "AND (:endDate IS NULL OR c.startDate <= :endDate) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Get challenge statistics for analytics
     */
    @Query("SELECT " +
            "COUNT(c) as totalChallenges, " +
            "COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as activeChallenges, " +
            "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedChallenges, " +
            "COUNT(CASE WHEN c.isPublic = true THEN 1 END) as publicChallenges, " +
            "AVG((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c)) as avgParticipants " +
            "FROM Challenge c")
    Object[] getChallengeStatistics();

    // ========================================================================
    // ADVANCED QUERIES - Business Intelligence
    // ========================================================================

    /**
     * Find creators with most successful challenges
     */
    @Query("SELECT c.creator, COUNT(c) as challengeCount, " +
            "AVG((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c)) as avgParticipants " +
            "FROM Challenge c " +
            "WHERE c.status = 'ACTIVE' " +
            "GROUP BY c.creator " +
            "HAVING COUNT(c) >= :minChallenges " +
            "ORDER BY avgParticipants DESC, challengeCount DESC")
    List<Object[]> findTopCreators(@Param("minChallenges") Long minChallenges, Pageable pageable);

    /**
     * Find challenge types with highest engagement
     */
    @Query("SELECT c.type, COUNT(c) as challengeCount, " +
            "AVG((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c)) as avgParticipants, " +
            "AVG((SELECT AVG(cp2.completionPercentage) FROM ChallengeProgress cp2 WHERE cp2.challenge = c)) as avgCompletion " +
            "FROM Challenge c " +
            "WHERE c.status = 'ACTIVE' " +
            "GROUP BY c.type " +
            "ORDER BY avgParticipants DESC, avgCompletion DESC")
    List<Object[]> findEngagementByType();

    /**
     * Find challenges that need attention (low participation or completion)
     */
    @Query("SELECT c FROM Challenge c " +
            "WHERE c.status = 'ACTIVE' " +
            "AND ((SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.challenge = c) < :minParticipants " +
            "     OR (SELECT AVG(cp2.completionPercentage) FROM ChallengeProgress cp2 WHERE cp2.challenge = c) < :minCompletion) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesNeedingAttention(
            @Param("minParticipants") Long minParticipants,
            @Param("minCompletion") Double minCompletion);

        /**
         * Find challenges by difficulty level
         */
        List<Challenge> findByDifficulty(ChallengeDifficulty difficulty);

        /**
         * Find active public challenges by difficulty
         */
        @Query("SELECT c FROM Challenge c WHERE " +
                "c.difficulty = :difficulty " +
                "AND c.status = 'ACTIVE' " +
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
                "AND c.status = 'ACTIVE' " +
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
                "AND c.status = 'ACTIVE' " +
                "ORDER BY c.difficulty DESC, c.startDate DESC")
        List<Challenge> findEasierThan(
                @Param("maxDifficulty") ChallengeDifficulty maxDifficulty,
                Pageable pageable);

        /**
         * Find challenges harder than specified difficulty
         */
        @Query("SELECT c FROM Challenge c WHERE " +
                "c.difficulty > :minDifficulty " +
                "AND c.status = 'ACTIVE' " +
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
                "c.difficulty = :difficulty AND c.status = 'ACTIVE'")
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
                "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCount " +
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
                "AND c.status = 'ACTIVE' " +
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

    /**
     * Find recommended challenges based on user's completed difficulty levels
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "AND FUNCTION('ORDINAL', c.difficulty) = (" +
            "    SELECT " +
            "    CASE " +
            "        WHEN AVG(FUNCTION('ORDINAL', cc.difficulty)) < 1 THEN 1 " +  // EASY
            "        WHEN AVG(FUNCTION('ORDINAL', cc.difficulty)) < 2 THEN 2 " +  // MEDIUM
            "        WHEN AVG(FUNCTION('ORDINAL', cc.difficulty)) < 3 THEN 3 " +  // HARD
            "        ELSE 4 " +                                                   // EXPERT
            "    END " +
            "    FROM Challenge cc " +
            "    JOIN cc.progress ccp " +
            "    WHERE ccp.user.id = :userId " +
            "    AND cc.status = com.my.challenger.entity.enums.ChallengeStatus.COMPLETED" +
            ") " +
            "AND NOT EXISTS (SELECT cp FROM c.progress cp WHERE cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findRecommendedByUserHistory(@Param("userId") Long userId, Pageable pageable);

    // Option 2: Alternative native query approach (more reliable)
    @Query(value = "SELECT c.* FROM challenges c WHERE " +
            "c.status = 'ACTIVE' " +
            "AND c.is_public = true " +
            "AND c.difficulty = (" +
            "    SELECT " +
            "    CASE " +
            "        WHEN AVG(CASE cc.difficulty " +
            "            WHEN 'BEGINNER' THEN 1 " +
            "            WHEN 'EASY' THEN 2 " +
            "            WHEN 'MEDIUM' THEN 3 " +
            "            WHEN 'HARD' THEN 4 " +
            "            WHEN 'EXPERT' THEN 5 " +
            "            WHEN 'EXTREME' THEN 6 " +
            "        END) < 2 THEN 'EASY' " +
            "        WHEN AVG(CASE cc.difficulty " +
            "            WHEN 'BEGINNER' THEN 1 " +
            "            WHEN 'EASY' THEN 2 " +
            "            WHEN 'MEDIUM' THEN 3 " +
            "            WHEN 'HARD' THEN 4 " +
            "            WHEN 'EXPERT' THEN 5 " +
            "            WHEN 'EXTREME' THEN 6 " +
            "        END) < 3 THEN 'MEDIUM' " +
            "        WHEN AVG(CASE cc.difficulty " +
            "            WHEN 'BEGINNER' THEN 1 " +
            "            WHEN 'EASY' THEN 2 " +
            "            WHEN 'MEDIUM' THEN 3 " +
            "            WHEN 'HARD' THEN 4 " +
            "            WHEN 'EXPERT' THEN 5 " +
            "            WHEN 'EXTREME' THEN 6 " +
            "        END) < 4 THEN 'HARD' " +
            "        ELSE 'EXPERT' " +
            "    END " +
            "    FROM challenges cc " +
            "    JOIN challenge_progress ccp ON cc.id = ccp.challenge_id " +
            "    WHERE ccp.user_id = :userId " +
            "    AND cc.status = 'COMPLETED'" +
            ") " +
            "AND NOT EXISTS (SELECT 1 FROM challenge_progress cp WHERE cp.challenge_id = c.id AND cp.user_id = :userId) " +
            "ORDER BY c.start_date DESC",
            nativeQuery = true)
    List<Challenge> findRecommendedByUserHistoryNative(@Param("userId") Long userId, Pageable pageable);

    // Option 3: Cleaner Java-based approach (Recommended)
// Move complex logic to service layer for better maintainability
    @Query("SELECT c FROM Challenge c WHERE " +
            "c.status = com.my.challenger.entity.enums.ChallengeStatus.ACTIVE " +
            "AND c.isPublic = true " +
            "AND c.difficulty = :recommendedDifficulty " +
            "AND NOT EXISTS (SELECT cp FROM c.progress cp WHERE cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findRecommendedByDifficulty(@Param("userId") Long userId,
                                                @Param("recommendedDifficulty") ChallengeDifficulty recommendedDifficulty,
                                                Pageable pageable);

    // Supporting method to get user's average difficulty
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
    Double getAverageDifficultyForUser(@Param("userId") Long userId);
}