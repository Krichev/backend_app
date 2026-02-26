package com.my.challenger.repository;

import com.my.challenger.entity.enums.AssignmentType;
import com.my.challenger.entity.quiz.ChallengeQuestionAssignment;
import com.my.challenger.entity.quiz.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeQuestionAssignmentRepository extends JpaRepository<ChallengeQuestionAssignment, Long> {

    /** Get all assignments for a challenge, ordered by sort_order */
    List<ChallengeQuestionAssignment> findByChallengeIdOrderBySortOrder(Long challengeId);

    /** Get just the questions for a challenge (skipping the assignment wrapper) */
    @Query("SELECT cqa.question FROM ChallengeQuestionAssignment cqa " +
           "WHERE cqa.challenge.id = :challengeId " +
           "ORDER BY cqa.sortOrder")
    List<QuizQuestion> findQuestionsByChallengeId(@Param("challengeId") Long challengeId);

    /** Get questions filtered by assignment type */
    @Query("SELECT cqa.question FROM ChallengeQuestionAssignment cqa " +
           "WHERE cqa.challenge.id = :challengeId " +
           "AND cqa.assignmentType = :type " +
           "ORDER BY cqa.sortOrder")
    List<QuizQuestion> findQuestionsByChallengeIdAndType(
            @Param("challengeId") Long challengeId,
            @Param("type") AssignmentType type);

    /** Count questions assigned to a challenge */
    long countByChallengeId(Long challengeId);

    /** Delete all assignments for a challenge */
    void deleteByChallengeId(Long challengeId);

    /** Check if a specific question is already assigned to a challenge */
    boolean existsByChallengeIdAndQuestionId(Long challengeId, Long questionId);
}
