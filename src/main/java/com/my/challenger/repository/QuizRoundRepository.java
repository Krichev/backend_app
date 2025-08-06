package com.my.challenger.repository;

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

}
