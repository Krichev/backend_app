// src/main/java/com/my/challenger/repository/QuizRoundRepository.java
package com.my.challenger.repository;

import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.quiz.QuizRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRoundRepository extends JpaRepository<QuizRound, Long> {

    /**
     * Find rounds by quiz session
     */
    List<QuizRound> findByQuizSessionIdOrderByRoundNumber(Long quizSessionId);

    /**
     * Find a specific round by session and round number
     */
    Optional<QuizRound> findByQuizSessionIdAndRoundNumber(Long quizSessionId, Integer roundNumber);

    /**
     * Count completed rounds for a session
     */
    long countByQuizSessionIdAndAnswerSubmittedAtIsNotNull(Long quizSessionId);

    /**
     * Count correct answers for a session
     */
    long countByQuizSessionIdAndIsCorrectTrue(Long quizSessionId);

    /**
     * Find rounds where a specific player answered
     */
    List<QuizRound> findByQuizSessionIdAndPlayerWhoAnswered(Long quizSessionId, String playerName);

    /**
     * Get statistics for a question across all sessions
     */
    @Query("SELECT COUNT(qr), SUM(CASE WHEN qr.isCorrect = true THEN 1 ELSE 0 END) " +
            "FROM QuizRound qr WHERE qr.question.id = :questionId")
    Object[] getQuestionStatistics(@Param("questionId") Long questionId);

    /**
     * Find rounds that used hints
     */
    List<QuizRound> findByQuizSessionIdAndHintUsedTrue(Long quizSessionId);

    /**
     * Find rounds that used voice recording
     */
    List<QuizRound> findByQuizSessionIdAndVoiceRecordingUsedTrue(Long quizSessionId);

    /**
     * Get average discussion time for completed rounds
     */
    @Query("SELECT AVG(qr.discussionDurationSeconds) FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId AND qr.discussionDurationSeconds IS NOT NULL")
    Double getAverageDiscussionTimeForSession(@Param("sessionId") Long sessionId);

    /**
     * Delete all rounds for a quiz session (for updating session configuration)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM QuizRound qr WHERE qr.quizSession.id = :sessionId")
    void deleteByQuizSessionId(@Param("sessionId") Long sessionId);

    /**
     * Get distinct questions that were played in a specific session, ordered by round number.
     * This is the source of truth for "what questions were in this quest?"
     */
    @Query("SELECT qr.question FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId " +
            "ORDER BY qr.roundNumber ASC")
    List<com.my.challenger.entity.quiz.QuizQuestion> findQuestionsBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Get distinct questions that were played across ANY completed session for a challenge.
     * Uses the latest completed session.
     */
    @Query("SELECT qr.question FROM QuizRound qr " +
            "WHERE qr.quizSession.challenge.id = :challengeId " +
            "AND qr.quizSession.status = 'COMPLETED' " +
            "AND qr.quizSession.id = (" +
            "  SELECT MAX(qs.id) FROM QuizSession qs " +
            "  WHERE qs.challenge.id = :challengeId AND qs.status = 'COMPLETED'" +
            ") " +
            "ORDER BY qr.roundNumber ASC")
    List<com.my.challenger.entity.quiz.QuizQuestion> findQuestionsFromLatestCompletedSession(@Param("challengeId") Long challengeId);

    // =====================================================================
    // MISSING METHODS THAT NEED TO BE ADDED
    // =====================================================================

    /**
     * Check if session has multimedia questions (questions that are NOT text-only)
     * This method was missing and causing compilation errors
     */
    @Query("SELECT CASE WHEN COUNT(qr) > 0 THEN true ELSE false END FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId AND qr.question.questionType <> :questionType")
    boolean existsByQuizSessionIdAndQuestionQuestionTypeNot(@Param("sessionId") Long sessionId,
                                                            @Param("questionType") QuestionType questionType);

    /**
     * Alternative method to check for multimedia questions
     */
    @Query("SELECT COUNT(qr) FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId AND qr.question.questionType <> 'TEXT'")
    long countMultimediaQuestionsBySession(@Param("sessionId") Long sessionId);

    /**
     * Find all rounds with multimedia questions for a session
     */
    @Query("SELECT qr FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId AND qr.question.questionType <> 'TEXT'")
    List<QuizRound> findMultimediaRoundsBySession(@Param("sessionId") Long sessionId);

    /**
     * Count rounds by question type for a session
     */
    @Query("SELECT COUNT(qr) FROM QuizRound qr " +
            "WHERE qr.quizSession.id = :sessionId AND qr.question.questionType = :questionType")
    long countBySessionIdAndQuestionType(@Param("sessionId") Long sessionId,
                                         @Param("questionType") QuestionType questionType);
}