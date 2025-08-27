// Additional repository for challenge-related statistics
package com.my.challenger.repository;
import com.my.challenger.entity.challenge.Challenge;
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
     * Find challenges by creator ID (without pagination)
     */
    List<Challenge> findByCreatorId(Long creatorId);

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
     * FIXED: Find challenges by participant ID using the new progress system
     * This replaces the old participants many-to-many relationship
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :participantId " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesByParticipantId(@Param("participantId") Long participantId, Pageable pageable);

    /**
     * FIXED: Find challenges by participant ID (without pagination)
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "JOIN c.progress cp " +
            "WHERE cp.user.id = :participantId " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesByParticipantId(@Param("participantId") Long participantId);

    /**
     * FIXED: Find challenges where user is creator OR participant using progress system
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "WHERE c.creator.id = :userId " +
            "OR EXISTS (SELECT cp FROM ChallengeProgress cp " +
            "           WHERE cp.challenge.id = c.id AND cp.user.id = :userId) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> findChallengesByUserIdAsCreatorOrParticipant(@Param("userId") Long userId, Pageable pageable);

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
     * Search challenges by keyword (without pagination)
     */
    @Query("SELECT c FROM Challenge c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY c.startDate DESC")
    List<Challenge> searchByKeyword(@Param("query") String query);

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
     * Find public challenges
     */
    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true ORDER BY c.startDate DESC")
    List<Challenge> findPublicChallenges(Pageable pageable);

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
     * Count challenges by creator
     */
    long countByCreatorId(Long creatorId);

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
     * Get user's challenge success rate
     */
    @Query("SELECT " +
            "CAST(COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) AS DOUBLE) / " +
            "CAST(COUNT(c) AS DOUBLE) * 100 " +
            "FROM Challenge c WHERE c.creator.id = :userId")
    Double getSuccessRateByCreatorId(@Param("userId") Long userId);
}