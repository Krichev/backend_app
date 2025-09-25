package com.my.challenger.repository;

import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    /**
     * Count all sessions by creator
     */
    long countByCreatorId(Long creatorId);

    /**
     * Find sessions by exact question source
     */
    List<QuizSession> findByCreatorIdAndQuestionSource(Long creatorId, String questionSource);

    /**
     * Find sessions by question source containing text
     */
    List<QuizSession> findByCreatorIdAndQuestionSourceContaining(Long creatorId, String questionSource);

    /**
     * FIXED: Replace the incorrectly named method with the correct creatorId-based method
     */
    @Query("SELECT s FROM QuizSession s WHERE s.creatorId = :creatorId ORDER BY s.createdAt DESC")
    List<QuizSession> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") Long creatorId, Pageable pageable);
    // Using the creatorId property
    List<QuizSession> findByCreatorId(Long creatorId);

    // Using both approaches (this works with either)
    @Query("SELECT s FROM QuizSession s WHERE s.creatorId = :creatorId")
    List<QuizSession> findSessionsByCreatorId(@Param("creatorId") Long creatorId);

//    // Join fetch for better performance
//    @Query("SELECT s FROM QuizSession s JOIN FETCH s.creator WHERE s.creatorId = :creatorId")
//    List<QuizSession> findByCreatorIdWithCreator(@Param("creatorId") Long creatorId);

//    @Query("SELECT s FROM QuizSession s JOIN FETCH s.quiz WHERE s.creatorId = :creatorId")
//    List<QuizSession> findByCreatorIdWithQuiz(@Param("creatorId") Long creatorId);

    // Additional useful methods for quiz session management
    List<QuizSession> findByStatus(QuizSessionStatus status);

    List<QuizSession> findByCreatorIdAndStatus(Long creatorId, QuizSessionStatus status);

    @Query("SELECT s FROM QuizSession s WHERE s.creatorId = :creatorId AND s.status = :status ORDER BY s.createdAt DESC")
    List<QuizSession> findByCreatorIdAndStatusOrderByCreatedAtDesc(@Param("creatorId") Long creatorId,
                                                                   @Param("status") QuizSessionStatus status);

    // Find sessions by question source (app or user)
    List<QuizSession> findByQuestionSource(String questionSource);

    // Find recent sessions for a creator
    @Query("SELECT s FROM QuizSession s WHERE s.creatorId = :creatorId AND s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<QuizSession> findRecentSessionsByCreator(@Param("creatorId") Long creatorId,
                                                  @Param("since") LocalDateTime since);

    // Count sessions by creator and status
    long countByCreatorIdAndStatus(Long creatorId, QuizSessionStatus status);

    // Find sessions with team name containing text
    List<QuizSession> findByCreatorIdAndTeamNameContainingIgnoreCase(Long creatorId, String teamName);

    // Custom query for finding sessions with specific criteria
    @Query("SELECT s FROM QuizSession s WHERE s.creatorId = :creatorId " +
            "AND (:questionSource IS NULL OR s.questionSource = :questionSource) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "ORDER BY s.createdAt DESC")
    List<QuizSession> findSessionsWithCriteria(@Param("creatorId") Long creatorId,
                                               @Param("questionSource") String questionSource,
                                               @Param("status") QuizSessionStatus status,
                                               Pageable pageable);

    // Delete sessions older than specified date
    @Modifying
    @Query("DELETE FROM QuizSession s WHERE s.createdAt < :cutoffDate")
    void deleteOldSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Update session status
    @Modifying
    @Query("UPDATE QuizSession s SET s.status = :newStatus, s.updatedAt = :updateTime WHERE s.id = :sessionId")
    void updateSessionStatus(@Param("sessionId") Long sessionId,
                             @Param("newStatus") QuizSessionStatus newStatus,
                             @Param("updateTime") LocalDateTime updateTime);


    /**
     * Find all quiz sessions for a specific challenge
     */
    List<QuizSession> findByChallengeId(Long challengeId);

    /**
     * Find quiz sessions for a challenge ordered by creation date (descending)
     */
    List<QuizSession> findByChallengeIdOrderByCreatedAtDesc(Long challengeId);

    /**
     * Find quiz sessions for a challenge with pagination
     */
    List<QuizSession> findByChallengeIdOrderByCreatedAtDesc(Long challengeId, Pageable pageable);

    List<QuizSession> findByHostUserIdAndStatus(Long userId, QuizSessionStatus status);

    List<QuizSession> findByHostUserId(Long userId);

    long countByHostUserId(Long userId);
}