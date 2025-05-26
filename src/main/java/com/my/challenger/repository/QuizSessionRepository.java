package com.my.challenger.repository;

import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.entity.quiz.QuizSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    /**
     * Find sessions by challenge ID
     */
    List<QuizSession> findByChallengeIdOrderByCreatedAtDesc(Long challengeId);

    /**
     * Find sessions by host user
     */
    List<QuizSession> findByHostUserIdOrderByCreatedAtDesc(Long hostUserId, Pageable pageable);

    /**
     * Find sessions by status
     */
    List<QuizSession> findByStatusOrderByCreatedAtDesc(QuizSessionStatus status);

    /**
     * Find active sessions for a challenge
     */
    List<QuizSession> findByChallengeIdAndStatus(Long challengeId, QuizSessionStatus status);

    /**
     * Find the most recent session for a challenge by a user
     */
    Optional<QuizSession> findFirstByChallengeIdAndHostUserIdOrderByCreatedAtDesc(Long challengeId, Long hostUserId);

    /**
     * Count completed sessions for a challenge
     */
    long countByChallengeIdAndStatus(Long challengeId, QuizSessionStatus status);

    /**
     * Find sessions completed in a date range
     */
    @Query("SELECT qs FROM QuizSession qs WHERE qs.status = 'COMPLETED' " +
            "AND qs.completedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY qs.completedAt DESC")
    List<QuizSession> findCompletedSessionsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find top scoring sessions for a challenge
     */
    @Query("SELECT qs FROM QuizSession qs WHERE qs.challengeId = :challengeId " +
            "AND qs.status = 'COMPLETED' " +
            "ORDER BY (CAST(qs.correctAnswers AS double) / qs.totalRounds) DESC")
    List<QuizSession> findTopScoringSessionsForChallenge(@Param("challengeId") Long challengeId, Pageable pageable);

    /**
     * Get average score for a challenge
     */
    @Query("SELECT AVG(CAST(qs.correctAnswers AS double) / qs.totalRounds * 100) " +
            "FROM QuizSession qs WHERE qs.challengeId = :challengeId AND qs.status = 'COMPLETED'")
    Double getAverageScorePercentageForChallenge(@Param("challengeId") Long challengeId);
}
