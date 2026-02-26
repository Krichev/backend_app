package com.my.challenger.repository;

import com.my.challenger.entity.quiz.ChallengeQuestionAssignment;
import com.my.challenger.entity.quiz.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeQuestionAssignmentRepository extends JpaRepository<ChallengeQuestionAssignment, Long> {

    List<ChallengeQuestionAssignment> findByChallengeIdOrderBySortOrder(Long challengeId);

    @Query("SELECT cqa.question FROM ChallengeQuestionAssignment cqa WHERE cqa.challenge.id = :challengeId ORDER BY cqa.sortOrder")
    List<QuizQuestion> findQuestionsByChallengeId(@Param("challengeId") Long challengeId);

    void deleteByChallengeId(Long challengeId);

    long countByChallengeId(Long challengeId);
}
