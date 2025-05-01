package com.my.challenger.repository;

import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.challenge.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChallengeProgress entity
 */
@Repository
public interface ChallengeProgressRepository extends JpaRepository<ChallengeProgress, Long> {

    /**
     * Check if a user has joined a challenge
     */
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    /**
     * Find challenge progress by challenge ID and user ID
     */
    Optional<ChallengeProgress> findByChallengeIdAndUserId(Long challengeId, Long userId);

    /**
     * Count participants for a challenge
     */
    Long countByChallenge(Challenge challenge);

    /**
     * Count participants for a challenge by ID
     */
    Long countByChallengeId(Long challengeId);

    /**
     * Find all challenges joined by a user
     */
    List<ChallengeProgress> findByUserId(Long userId);

    /**
     * Find all users participating in a challenge
     */
    @Query("SELECT cp FROM ChallengeProgress cp WHERE cp.challenge.id = :challengeId")
    List<ChallengeProgress> findParticipantsByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * Find challenges with highest completion percentage for a user
     */
    @Query("SELECT cp FROM ChallengeProgress cp WHERE cp.user.id = :userId " +
            "ORDER BY cp.completionPercentage DESC")
    List<ChallengeProgress> findTopChallengesByCompletionPercentage(@Param("userId") Long userId);

    /**
     * Count completed challenges for a user
     */
    @Query("SELECT COUNT(cp) FROM ChallengeProgress cp WHERE cp.user.id = :userId AND cp.status = 'COMPLETED'")
    Long countCompletedChallengesByUserId(@Param("userId") Long userId);
}