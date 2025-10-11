package com.my.challenger.repository;

import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    Optional<QuizQuestion> findByLegacyQuestionId(Integer legacyQuestionId);

    Optional<QuizQuestion> findByExternalId(String externalId);

    List<QuizQuestion> findByTopic_NameAndIsActiveTrue(String topic);

    List<QuizQuestion> findByDifficultyAndIsActiveTrue(QuizDifficulty difficulty);

    Page<QuizQuestion> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT qq FROM QuizQuestion qq WHERE " +
            "qq.isActive = true AND " +
            "(:topic IS NULL OR qq.topic.name = :topic) AND " +
            "(:difficulty IS NULL OR qq.difficulty = :difficulty) AND " +
            "(:questionType IS NULL OR qq.questionType = :questionType)")
    Page<QuizQuestion> searchQuestions(
            @Param("topic") String topic,
            @Param("difficulty") QuizDifficulty difficulty,
            @Param("questionType") QuestionType questionType,
            Pageable pageable);

    @Query("SELECT qq FROM QuizQuestion qq " +
            "WHERE qq.id NOT IN " +
            "(SELECT q.quizQuestion.id FROM Question q WHERE q.tournamentId = :tournamentId)")
    List<QuizQuestion> findQuestionsNotInTournament(@Param("tournamentId") Integer tournamentId);

    @Query("SELECT qq FROM QuizQuestion qq " +
            "WHERE qq.isActive = true " +
            "ORDER BY qq.usageCount DESC")
    Page<QuizQuestion> findMostUsedQuestions(Pageable pageable);

    @Query("SELECT qq FROM QuizQuestion qq " +
            "LEFT JOIN FETCH qq.tournamentQuestions " +
            "WHERE qq.id = :id")
    Optional<QuizQuestion> findByIdWithUsages(@Param("id") Long id);

    /**
     * Find questions by creator ID ordered by creation time
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") Long creatorId);

    /**
     * Find questions by topic
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.topic.name = :topic ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByTopicOrderByCreatedAtDesc(@Param("topic") String topic);

    /**
     * Find questions by source
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.source = :source ORDER BY q.createdAt DESC")
    List<QuizQuestion> findBySourceOrderByCreatedAtDesc(@Param("source") String source);

    /**
     * Find questions by user created flag
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.isUserCreated = :isUserCreated ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByIsUserCreatedOrderByCreatedAtDesc(@Param("isUserCreated") Boolean isUserCreated);

    /**
     * Find questions by difficulty
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.difficulty = :difficulty ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByDifficultyOrderByCreatedAtDesc(@Param("difficulty") QuizDifficulty difficulty);

    // ========== MULTIMEDIA METHODS ==========

    /**
     * Find questions by question type
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionType = :questionType ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByQuestionTypeOrderByCreatedAtDesc(@Param("questionType") QuestionType questionType, Pageable pageable);

    /**
     * Find questions by media type
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaType = :mediaType ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByQuestionMediaTypeOrderByCreatedAtDesc(@Param("mediaType") MediaType mediaType, Pageable pageable);

    /**
     * Find questions by question type and difficulty
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionType = :questionType AND q.difficulty = :difficulty ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByQuestionTypeAndDifficulty(@Param("questionType") QuestionType questionType,
                                                       @Param("difficulty") QuizDifficulty difficulty,
                                                       Pageable pageable);

    // ========== SEARCH METHODS (FIXED) ==========

    /**
     * Search questions by keyword across multiple fields
     * This is the FIXED method that replaces the problematic searchByKeyword
     */
    @Query("SELECT q FROM QuizQuestion q WHERE " +
            "LOWER(q.question) LIKE %:keyword% OR " +
            "LOWER(q.answer) LIKE %:keyword% OR " +
            "LOWER(q.topic) LIKE %:keyword% OR " +
            "LOWER(q.source) LIKE %:keyword% OR " +
            "LOWER(q.additionalInfo) LIKE %:keyword% " +
            "ORDER BY q.createdAt DESC")
    List<QuizQuestion> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * BEST SOLUTION #2: Advanced search with clean syntax
     */
    @Query("SELECT q FROM QuizQuestion q " +
            "LEFT JOIN q.topic t " +
            "WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "   LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(q.answer) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(q.source) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(CAST(:difficulty AS string) IS NULL OR q.difficulty = :difficulty) AND " +
            "(:topic IS NULL OR :topic = '' OR LOWER(t.name) = LOWER(:topic)) AND " +
            "(:isUserCreated IS NULL OR q.isUserCreated = :isUserCreated) " +
            "ORDER BY q.createdAt DESC")
    Page<QuizQuestion> searchWithFilters(@Param("keyword") String keyword,
                                         @Param("difficulty") QuizDifficulty difficulty,
                                         @Param("topic") String topic,
                                         @Param("isUserCreated") Boolean isUserCreated,
                                         Pageable pageable);
    /**
     * Search questions by keyword in specific field
     */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(q.question) LIKE (CONCAT('%', :keyword, '%')) ORDER BY q.createdAt DESC")
    List<QuizQuestion> searchByQuestionText(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Search questions by topic pattern
     */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(q.topic.name) LIKE (CONCAT('%', :topicPattern, '%')) ORDER BY q.createdAt DESC")
    List<QuizQuestion> searchByTopicPattern(@Param("topicPattern") String topicPattern, Pageable pageable);

    // ========== STATISTICS AND ANALYTICS ==========

    /**
     * Get question count by difficulty
     */
    @Query("SELECT q.difficulty, COUNT(q) FROM QuizQuestion q GROUP BY q.difficulty ORDER BY q.difficulty")
    List<Object[]> getQuestionCountByDifficulty();

    /**
     * Get question count by topic
     */
    @Query("SELECT q.topic, COUNT(q) FROM QuizQuestion q WHERE q.topic IS NOT NULL GROUP BY q.topic ORDER BY COUNT(q) DESC")
    List<Object[]> getQuestionCountByTopic();

    /**
     * Get most used questions
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.usageCount > 0 ORDER BY q.usageCount DESC")
    List<QuizQuestion> getMostUsedQuestions(Pageable pageable);

    /**
     * Get questions by creator with pagination
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId ORDER BY q.createdAt DESC")
    Page<QuizQuestion> findByCreatorId(@Param("creatorId") Long creatorId, Pageable pageable);

    // ========== RANDOM SELECTION METHODS ==========

    /**
     * Get random questions by difficulty
     */
    @Query(value = "SELECT * FROM quiz_questions q WHERE q.difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<QuizQuestion> findRandomByDifficulty(@Param("difficulty") String difficulty, @Param("limit") int limit);

    /**
     * Get random questions from multiple difficulties
     */
    @Query(value = "SELECT * FROM quiz_questions q WHERE q.difficulty IN :difficulties ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<QuizQuestion> findRandomByDifficulties(@Param("difficulties") List<String> difficulties, @Param("limit") int limit);

    /**
     * Get random questions by topic
     */
    @Query(value = "SELECT * FROM quiz_questions q WHERE q.topic.name = :topic ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<QuizQuestion> findRandomByTopic(@Param("topic") String topic, @Param("limit") int limit);

    // ========== UTILITY METHODS ==========

    /**
     * Count questions by creator
     */
    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.creator.id = :creatorId")
    long countByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * Find questions created after specific date
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.createdAt >= :date ORDER BY q.createdAt DESC")
    List<QuizQuestion> findQuestionsCreatedAfter(@Param("date") LocalDateTime date, Pageable pageable);

    /**
     * Find unused questions (never used in quiz)
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.usageCount = 0 ORDER BY q.createdAt DESC")
    List<QuizQuestion> findUnusedQuestions(Pageable pageable);

    /**
     * Update usage count for question
     */
    @Modifying
    @Query("UPDATE QuizQuestion q SET q.usageCount = q.usageCount + 1 WHERE q.id = :questionId")
    void incrementUsageCount(@Param("questionId") Long questionId);


    // ========== MULTIMEDIA SPECIFIC QUERIES ==========

    /**
     * Find questions with media
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaUrl IS NOT NULL ORDER BY q.createdAt DESC")
    List<QuizQuestion> findQuestionsWithMedia(Pageable pageable);

    /**
     * Find questions without media (text only)
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaUrl IS NULL AND q.questionType = com.my.challenger.entity.enums.QuestionType.TEXT ORDER BY q.createdAt DESC")
    List<QuizQuestion> findTextOnlyQuestions(Pageable pageable);

    /**
     * Find questions by media type and difficulty
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.questionMediaType = :mediaType AND q.difficulty = :difficulty ORDER BY q.createdAt DESC")
    List<QuizQuestion> findByMediaTypeAndDifficulty(@Param("mediaType") MediaType mediaType,
                                                    @Param("difficulty") QuizDifficulty difficulty,
                                                    Pageable pageable);

    // ========== CONTENT VALIDATION ==========

    /**
     * Find duplicate questions by text similarity
     */
    @Query("SELECT q FROM QuizQuestion q WHERE LOWER(q.question) = (:questionText)")
    List<QuizQuestion> findDuplicateQuestions(@Param("questionText") String questionText);

    /**
     * Find questions with missing required fields
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.question IS NULL OR q.answer IS NULL OR TRIM(q.question) = '' OR TRIM(q.answer) = ''")
    List<QuizQuestion> findQuestionsWithMissingFields();

    List<QuizQuestion> findByCreatorIdAndIsUserCreated(Long userId, boolean b);

    List<QuizQuestion> findByDifficulty(QuizDifficulty difficulty);

    List<QuizQuestion> findByCreatorIdAndSourceContaining(Long id, String s);

    List<QuizQuestion> findByDifficulty(QuizDifficulty difficulty, Pageable pageable);

    Long countByCreatorIdAndSourceContaining(Long id, String s);

    List<QuizQuestion> findByDifficultyOrderByUsageCountAsc(@NotNull(message = "Difficulty is required") QuizDifficulty difficulty, PageRequest pageRequest);

    List<QuizQuestion> findByCreatorIdAndIsUserCreatedTrueOrderByCreatedAtDesc(Long userId);

    long countByDifficulty(QuizDifficulty difficulty);
}
