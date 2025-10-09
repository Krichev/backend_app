package com.my.challenger.repository;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.quiz.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {


    /**
     * Find all questions for a tournament
     * Orders by display order ascending
     *
     * @param tournamentId The tournament ID
     * @return List of questions for the tournament
     */
    @Query("SELECT q FROM Question q WHERE q.tournamentId = :tournamentId ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentId(@Param("tournamentId") Integer tournamentId);

    /**
     * Find all questions for a tournament with QuizQuestion data eagerly loaded
     * Used by the controller to avoid N+1 query problem
     *
     * @param tournamentId The tournament ID
     * @return List of questions with QuizQuestion data
     */
    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.quizQuestion " +
            "WHERE q.tournamentId = :tournamentId " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentIdWithQuizQuestion(@Param("tournamentId") Integer tournamentId);

    /**
     * Find a single question by ID with QuizQuestion data
     *
     * @param id The question ID
     * @return Optional containing the question if found
     */
    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.quizQuestion " +
            "WHERE q.id = :id")
    Optional<Question> findByIdWithQuizQuestion(@Param("id") Integer id);

    /**
     * Find active questions by tournament and difficulty
     * Used specifically for filtering questions for games
     *
     * @param tournamentId The tournament ID
     * @param difficulty   The difficulty level
     * @return List of active questions with the specified difficulty
     */
    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.quizQuestion qq " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true " +
            "AND qq.difficulty = :difficulty " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findActiveByTournamentIdAndDifficulty(
            @Param("tournamentId") Integer tournamentId,
            @Param("difficulty") QuizDifficulty difficulty);

    /**
     * Count active questions by tournament and difficulty
     *
     * @param tournamentId The tournament ID
     * @param difficulty   The difficulty level
     * @return Count of active questions
     */
    @Query("SELECT COUNT(q) FROM Question q " +
            "JOIN q.quizQuestion qq " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true " +
            "AND qq.difficulty = :difficulty")
    long countActiveByTournamentIdAndDifficulty(
            @Param("tournamentId") Integer tournamentId,
            @Param("difficulty") QuizDifficulty difficulty);

    /**
     * Find all active questions for a tournament
     *
     * @param tournamentId The tournament ID
     * @param isActive     Active status
     * @return List of active questions
     */
    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.quizQuestion " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = :isActive " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentIdAndIsActive(
            @Param("tournamentId") Integer tournamentId,
            @Param("isActive") Boolean isActive);

    /**
     * Get the maximum display order for a tournament
     * Used when adding new questions
     *
     * @param tournamentId The tournament ID
     * @return Maximum display order, or null if no questions exist
     */
    @Query("SELECT MAX(q.displayOrder) FROM Question q WHERE q.tournamentId = :tournamentId")
    Integer findMaxDisplayOrderByTournamentId(@Param("tournamentId") Integer tournamentId);

    /**
     * Check if a quiz question is already added to a tournament
     *
     * @param tournamentId   The tournament ID
     * @param quizQuestionId The quiz question ID
     * @return true if the question exists in the tournament
     */
    @Query("SELECT COUNT(q) > 0 FROM Question q " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.quizQuestion.id = :quizQuestionId")
    boolean existsByTournamentIdAndQuizQuestionId(
            @Param("tournamentId") Integer tournamentId,
            @Param("quizQuestionId") Long quizQuestionId);

    /**
     * Delete all questions for a tournament
     * WARNING: Use with caution
     *
     * @param tournamentId The tournament ID
     */
    void deleteByTournamentId(@Param("tournamentId") Integer tournamentId);

    /**
     * Count total questions in a tournament
     *
     * @param tournamentId The tournament ID
     * @return Total count of questions
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.tournamentId = :tournamentId")
    long countByTournamentId(@Param("tournamentId") Integer tournamentId);

    /**
     * Count active questions in a tournament
     *
     * @param tournamentId The tournament ID
     * @return Count of active questions
     */
    @Query("SELECT COUNT(q) FROM Question q " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true")
    long countActiveByTournamentId(@Param("tournamentId") Integer tournamentId);


    // Find by tournament with proper ordering
    @Query("SELECT q FROM Question q " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentIdOrderByDisplayOrder(@Param("tournamentId") Integer tournamentId);

//    // Find with quiz question eagerly loaded
//    @Query("SELECT q FROM Question q " +
//            "LEFT JOIN FETCH q.quizQuestion " +
//            "WHERE q.tournamentId = :tournamentId " +
//            "AND q.isActive = true " +
//            "ORDER BY q.displayOrder ASC")
//    List<Question> findByTournamentIdWithQuizQuestion(@Param("tournamentId") Integer tournamentId);

    // Get next available display order for tournament
    @Query("SELECT COALESCE(MAX(q.displayOrder), 0) + 1 FROM Question q " +
            "WHERE q.tournamentId = :tournamentId")
    Integer getNextDisplayOrder(@Param("tournamentId") Integer tournamentId);

    // Check if display order exists in tournament
    boolean existsByTournamentIdAndDisplayOrder(Integer tournamentId, Integer displayOrder);

    // Find questions using a specific quiz question
    @Query("SELECT q FROM Question q WHERE q.quizQuestion.id = :quizQuestionId")
    List<Question> findByQuizQuestionId(@Param("quizQuestionId") Long quizQuestionId);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.quizQuestion.id = :quizQuestionId")
    Long countUsageByQuizQuestionId(@Param("quizQuestionId") Long quizQuestionId);

//    boolean existsByTournamentIdAndQuizQuestionId(Integer tournamentId, Long quizQuestionId);
}