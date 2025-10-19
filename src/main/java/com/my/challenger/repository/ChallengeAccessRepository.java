package com.my.challenger.repository;

import com.my.challenger.entity.challenge.ChallengeAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeAccessRepository extends JpaRepository<ChallengeAccess, Long> {

    /**
     * Check if a user has access to a challenge
     */
    @Query("SELECT CASE WHEN COUNT(ca) > 0 THEN true ELSE false END " +
           "FROM ChallengeAccess ca " +
           "WHERE ca.challenge.id = :challengeId " +
           "AND ca.user.id = :userId " +
           "AND ca.status = 'ACTIVE'")
    boolean hasAccess(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

    /**
     * Find access record by challenge and user
     */
    Optional<ChallengeAccess> findByChallengeIdAndUserId(Long challengeId, Long userId);

    /**
     * Get all users with access to a challenge
     */
    @Query("SELECT ca FROM ChallengeAccess ca " +
           "WHERE ca.challenge.id = :challengeId " +
           "AND ca.status = 'ACTIVE'")
    List<ChallengeAccess> findActiveByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * Get all challenges a user has access to
     */
    @Query("SELECT ca FROM ChallengeAccess ca " +
           "WHERE ca.user.id = :userId " +
           "AND ca.status = 'ACTIVE'")
    List<ChallengeAccess> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Count users with access to a challenge
     */
    long countByChallengeIdAndStatus(Long challengeId, String status);

    /**
     * Delete all access records for a challenge
     */
    void deleteByChallengeId(Long challengeId);

    /**
     * Delete specific access record
     */
    void deleteByChallengeIdAndUserId(Long challengeId, Long userId);
}