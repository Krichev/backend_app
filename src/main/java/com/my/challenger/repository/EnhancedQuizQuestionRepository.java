package com.my.challenger.repository;


import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.entity.quiz.QuizSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnhancedQuizQuestionRepository extends QuizQuestionRepository {
    
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

}