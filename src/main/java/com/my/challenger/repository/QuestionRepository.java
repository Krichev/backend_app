package com.my.challenger.repository;

import com.my.challenger.entity.quiz.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    // Find by tournament with proper ordering
    @Query("SELECT q FROM Question q " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentIdOrderByDisplayOrder(@Param("tournamentId") Integer tournamentId);

    // Find with quiz question eagerly loaded
    @Query("SELECT q FROM Question q " +
            "LEFT JOIN FETCH q.quizQuestion " +
            "WHERE q.tournamentId = :tournamentId " +
            "AND q.isActive = true " +
            "ORDER BY q.displayOrder ASC")
    List<Question> findByTournamentIdWithQuizQuestion(@Param("tournamentId") Integer tournamentId);

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

    boolean existsByTournamentIdAndQuizQuestionId(Integer tournamentId, Long quizQuestionId);
}