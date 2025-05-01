package com.my.challenger.repository;

import com.my.challenger.entity.challenge.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.my.challenger.entity.challenge.Challenge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Challenge entity
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
            "(:visibility IS NULL OR c.isPublic = :visibility) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:targetGroup IS NULL OR c.group.name = :targetGroup)")
    List<Challenge> findWithFilters(
            @Param("type") String type,
            @Param("visibility") String visibility,
            @Param("status") String status,
            @Param("targetGroup") String targetGroup,
            Pageable pageable);

    /**
     * Find challenges by participant ID
     */
    @Query("SELECT c FROM Challenge c JOIN ChallengeProgress cp ON c.id = cp.challenge.id " +
            "WHERE cp.user.id = :participantId")
    List<Challenge> findChallengesByParticipantId(@Param("participantId") Long participantId, Pageable pageable);

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
}