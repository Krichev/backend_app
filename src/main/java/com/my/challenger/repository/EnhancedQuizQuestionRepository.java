package com.my.challenger.repository;


import com.my.challenger.entity.quiz.QuizQuestion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnhancedQuizQuestionRepository extends QuizQuestionRepository {
    
    /**
     * Find questions by creator and question text to avoid duplicates
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.question = :questionText")
    List<QuizQuestion> findByCreator_IdAndQuestionText(@Param("creatorId") Long creatorId,
                                                       @Param("questionText") String questionText);

    /**
     * Find user questions by source (to get questions created for specific challenges)
     */
    @Query("SELECT q FROM QuizQuestion q WHERE q.creator.id = :creatorId AND q.source LIKE %:sourcePattern%")
    List<QuizQuestion> findByCreator_IdAndSourceContaining(@Param("creatorId") Long creatorId,
                                                           @Param("sourcePattern") String sourcePattern);

}