// src/main/java/com/my/challenger/repository/QuizQuestionRepository.java
package com.my.challenger.repository;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    // Existing methods...
    List<QuizQuestion> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
    List<QuizQuestion> findByTopicOrderByCreatedAtDesc(String topic);
    List<QuizQuestion> findBySourceOrderByCreatedAtDesc(String source);
    List<QuizQuestion> findByIsUserCreatedOrderByCreatedAtDesc(Boolean isUserCreated);
    List<QuizQuestion> findByDifficultyOrderByCreatedAtDesc(QuizDifficulty difficulty);

    // Updated multimedia-specific methods with MediaType enum
    List<QuizQuestion> findByQuestionTypeOrderByCreatedAtDesc(QuestionType questionType, Pageable pageable);

    List<QuizQuestion> findByQuestionMediaTypeOrderByCreatedAtDesc(MediaType mediaType, Pageable pageable);

    List<QuizQuestion> findByQuestionTypeAndQuestionMediaTypeOrderByCreatedAtDesc(
            QuestionType questionType, MediaType mediaType, Pageable pageable);

    List<QuizQuestion> findByCreatorIdAndQuestionTypeNotOrderByCreatedAtDesc(Long creatorId, QuestionType questionType);

    List<QuizQuestion> findByQuestionTypeAndIsUserCreatedOrderByCreatedAtDesc(
            QuestionType questionType, Boolean isUserCreated);

    boolean existsByQuestionMediaId(String questionMediaId);

    // Updated count methods with enum support
    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.questionType != :questionType")
    Long countByQuestionTypeNot(@Param("questionType") QuestionType questionType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.questionType = :questionType")
    Long countByQuestionType(@Param("questionType") QuestionType questionType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.questionMediaType = :mediaType")
    Long countByQuestionMediaType(@Param("mediaType") MediaType mediaType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.questionType = :questionType")
    Long countByCreatorIdAndQuestionType(@Param("creatorId") Long creatorId, @Param("questionType") QuestionType questionType);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.questionMediaType = :mediaType")
    Long countByCreatorIdAndQuestionMediaType(@Param("creatorId") Long creatorId, @Param("mediaType") MediaType mediaType);

    // Media-related queries
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaUrl IS NOT NULL")
    List<QuizQuestion> findQuestionsWithMedia();

    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaId IS NULL AND q.questionType != 'TEXT'")
    List<QuizQuestion> findInconsistentMultimediaQuestions();

    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaType IS NOT NULL AND q.questionMediaUrl IS NULL")
    List<QuizQuestion> findQuestionsWithMediaTypeButNoUrl();

    // Advanced filtering with multiple enum parameters
    @Query("""
            SELECT q FROM QuizQuestion q 
            WHERE q.creator.id = :creatorId 
            AND (:questionType IS NULL OR q.questionType = :questionType)
            AND (:mediaType IS NULL OR q.questionMediaType = :mediaType)
            AND (:isUserCreated IS NULL OR q.isUserCreated = :isUserCreated)
            ORDER BY q.createdAt DESC
            """)
    List<QuizQuestion> findUserQuestionsFiltered(
            @Param("creatorId") Long creatorId,
            @Param("questionType") QuestionType questionType,
            @Param("mediaType") MediaType mediaType,
            @Param("isUserCreated") Boolean isUserCreated,
            Pageable pageable
    );

    // Statistics queries
    @Query("""
            SELECT q.questionType, q.questionMediaType, COUNT(q) 
            FROM QuizQuestion q 
            GROUP BY q.questionType, q.questionMediaType
            ORDER BY COUNT(q) DESC
            """)
    List<Object[]> getQuestionTypeMediaTypeStats();

    @Query("""
            SELECT q.questionType, COUNT(q) 
            FROM QuizQuestion q 
            WHERE q.creator.id = :creatorId
            GROUP BY q.questionType
            ORDER BY COUNT(q) DESC
            """)
    List<Object[]> getUserQuestionTypeStats(@Param("creatorId") Long creatorId);

    // Search with enum filters
    @Query("""
            SELECT q FROM QuizQuestion q 
            WHERE LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND (:questionType IS NULL OR q.questionType = :questionType)
            AND (:mediaType IS NULL OR q.questionMediaType = :mediaType)
            ORDER BY q.createdAt DESC
            """)
    List<QuizQuestion> searchQuestionsWithFilters(
            @Param("keyword") String keyword,
            @Param("questionType") QuestionType questionType,
            @Param("mediaType") MediaType mediaType,
            Pageable pageable
    );

    List<QuizQuestion> findByCreatorIdAndIsUserCreated(Long userId, boolean b);

    List<QuizQuestion> findByDifficultyOrderByRandom(QuizDifficulty difficulty);

    List<QuizQuestion> findByCreatorIdAndSourceContaining(Long id, String s);

    List<QuizQuestion> findRandomQuestionsByDifficulty(String name, int remainingCount);

    Long countByCreatorIdAndSourceContaining(Long id, String s);

    List<QuizQuestion> findByDifficultyOrderByUsageCountAsc(@NotNull(message = "Difficulty is required") QuizDifficulty difficulty, PageRequest pageRequest);

    List<QuizQuestion> findByCreatorIdAndIsUserCreatedTrueOrderByCreatedAtDesc(Long userId);

    List<QuizQuestion> searchByKeyword(String keyword, PageRequest of);
}