// src/main/java/com/my/challenger/repository/QuizQuestionRepository.java
package com.my.challenger.repository;

import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {


    List<QuizQuestion> findByCreatorIdAndIsActiveTrue(Long creatorId);

    List<QuizQuestion> findByDifficultyAndIsActiveTrue(QuizDifficulty difficulty);

    @Query("SELECT q FROM QuizQuestion q WHERE q.difficulty = :difficulty AND q.isActive = true ORDER BY RANDOM()")
    List<QuizQuestion> findByDifficultyOrderByRandom(@Param("difficulty") QuizDifficulty difficulty);

    @Query("SELECT q FROM QuizQuestion q WHERE q.question LIKE %:keyword% OR q.topic LIKE %:keyword% AND q.isActive = true")
    List<QuizQuestion> findByKeywordContaining(@Param("keyword") String keyword);

    List<QuizQuestion> findByCreatorIdAndDifficultyAndIsActiveTrue(Long creatorId, QuizDifficulty difficulty);

    List<QuizQuestion> findBySourceAndIsActiveTrue(String source);

    List<QuizQuestion> findByExternalIdAndIsActiveTrue(String externalId);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.isActive = true")
    Long countByCreatorIdAndIsActiveTrue(@Param("creatorId") Long creatorId);

    // Alternative if FUNCTION('RANDOM') doesn't work in your DB:
    @Query(value = "SELECT * FROM quiz_questions q WHERE q.difficulty = :difficulty " +
            "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizQuestion> findRandomQuestionsByDifficultyNative(@Param("difficulty") String difficulty,
                                                             @Param("limit") int limit);


    /**
     * Find questions by difficulty
     */
    List<QuizQuestion> findByDifficultyOrderByUsageCountAsc(QuizDifficulty difficulty, Pageable pageable);

    /**
     * Find user-created questions by creator
     */
    List<QuizQuestion> findByCreatorIdAndIsUserCreatedTrueOrderByCreatedAtDesc(Long creatorId);

    /**
     * Find all user-created questions
     */
    List<QuizQuestion> findByIsUserCreatedTrueOrderByCreatedAtDesc();

    /**
     * Search questions by keyword in question text
     */
    @Query("SELECT q FROM QuizQuestion q WHERE " +
            "LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(q.topic) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<QuizQuestion> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find random questions by difficulty (excluding recently used ones)
     */
    @Query(value = "SELECT * FROM quiz_questions q WHERE q.difficulty = :difficulty " +
            "AND (q.last_used IS NULL OR q.last_used < DATEADD(HOUR, -24, CURRENT_TIMESTAMP)) " +
            "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<QuizQuestion> findRandomQuestionsByDifficulty(@Param("difficulty") String difficulty, @Param("limit") int limit);

    /**
     * Find questions by external ID (for questions from APIs)
     */
    List<QuizQuestion> findByExternalId(String externalId);

    /**
     * Count user-created questions by creator
     */
    long countByCreatorIdAndIsUserCreatedTrue(Long creatorId);

    /**
     * Find most used questions
     */
    List<QuizQuestion> findTop10ByOrderByUsageCountDesc();

    /**
     * Find questions by creator and question text to avoid duplicates
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.question = :questionText")
    List<QuizQuestion> findByCreatorIdAndQuestionText(@Param("creatorId") Long creatorId,
                                                      @Param("questionText") String questionText);

    /**
     * Find user questions by source (to get questions created for specific challenges)
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.source LIKE %:sourcePattern%")
    List<QuizQuestion> findByCreatorIdAndSourceContaining(@Param("creatorId") Long creatorId,
                                                          @Param("sourcePattern") String sourcePattern);

//    /**
//     * Get questions by multiple difficulties for mixed quizzes
//     */
//    @Query("SELECT q FROM QuizQuestion q WHERE q.difficulty IN :difficulties " +
//            "AND (q.lastUsed IS NULL OR q.lastUsed < :cutoffTime) " +
//            "ORDER BY FUNCTION('RANDOM')")
//    List<QuizQuestion> findRandomQuestionsByDifficulties(@Param("difficulties") List<QuizDifficulty> difficulties,
//                                                         @Param("cutoffTime") LocalDateTime cutoffTime,
//                                                         Pageable pageable);

    /**
     * Find sessions by challenge ID (without ordering)
     */
    List<QuizSession> findByChallengeId(Long challengeId);

// Additional methods to add to QuizRoundRepository
// Add these methods to your existing QuizRoundRepository interface:

    /**
     * Delete all rounds for a quiz session (for updating session configuration)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM QuizRound qr WHERE qr.quizSession.id = :sessionId")
    void deleteByQuizSessionId(@Param("sessionId") Long sessionId);

// Additional methods to add to QuizQuestionRepository
// Add this method to your existing QuizQuestionRepository interface:

    /**
     * Count questions by creator and source containing specific text
     */
    long countByCreatorIdAndSourceContaining(Long creatorId, String sourceText);

    List<QuizQuestion> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

//    List<QuizQuestion> findByIsUserCreatedTrueOrderByCreatedAtDesc();

    List<QuizQuestion> findByDifficultyOrderByCreatedAtDesc(
            com.my.challenger.entity.enums.QuizDifficulty difficulty
    );

    // New multimedia-specific methods
    List<QuizQuestion> findByQuestionTypeOrderByCreatedAtDesc(QuestionType questionType, Pageable pageable);

    List<QuizQuestion> findByCreatorIdAndQuestionTypeNotOrderByCreatedAtDesc(Long creatorId, QuestionType questionType);

    List<QuizQuestion> findByQuestionTypeAndIsUserCreatedOrderByCreatedAtDesc(
            QuestionType questionType, Boolean isUserCreated
    );

    boolean existsByQuestionMediaId(String questionMediaId);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.questionType != :questionType")
    Long countByQuestionTypeNot(@Param("questionType") QuestionType questionType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.questionType = :questionType")
    Long countByQuestionType(@Param("questionType") QuestionType questionType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.creatorId = :creatorId AND q.questionType = :questionType")
    Long countByCreatorIdAndQuestionType(@Param("creatorId") Long creatorId, @Param("questionType") QuestionType questionType);

    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaUrl IS NOT NULL")
    List<QuizQuestion> findQuestionsWithMedia();

    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaId IS NULL AND q.questionType != 'TEXT'")
    List<QuizQuestion> findInconsistentMultimediaQuestions();

    @Query("""
            SELECT q FROM QuizQuestion q 
            WHERE q.creator.id = :creatorId 
            AND (:questionType IS NULL OR q.questionType = :questionType)
            AND (:isUserCreated IS NULL OR q.isUserCreated = :isUserCreated)
            ORDER BY q.createdAt DESC
            """)
    List<QuizQuestion> findUserQuestionsFiltered(
            @Param("creatorId") Long creatorId,
            @Param("questionType") QuestionType questionType,
            @Param("isUserCreated") Boolean isUserCreated,
            Pageable pageable
    );
}

