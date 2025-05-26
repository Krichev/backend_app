// src/main/java/com/my/challenger/repository/QuizQuestionRepository.java
package com.my.challenger.repository;

import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

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
}

