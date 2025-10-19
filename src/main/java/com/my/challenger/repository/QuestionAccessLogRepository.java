package com.my.challenger.repository;

import com.my.challenger.entity.enums.QuestionAccessType;
import com.my.challenger.entity.quiz.QuestionAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionAccessLogRepository extends JpaRepository<QuestionAccessLog, Long> {

    /**
     * Find all access logs for a question
     */
    List<QuestionAccessLog> findByQuestionIdOrderByAccessedAtDesc(Long questionId);

    /**
     * Find access logs by user
     */
    List<QuestionAccessLog> findByAccessedByUserIdOrderByAccessedAtDesc(Long userId);

    /**
     * Find recent usage of a question
     */
    @Query("SELECT qal FROM QuestionAccessLog qal WHERE " +
            "qal.question.id = :questionId " +
            "AND qal.accessType = :accessType " +
            "AND qal.accessedAt >= :since")
    List<QuestionAccessLog> findRecentUsage(@Param("questionId") Long questionId,
                                            @Param("accessType") QuestionAccessType accessType,
                                            @Param("since") LocalDateTime since);

    /**
     * Count unique users who accessed a question
     */
    @Query("SELECT COUNT(DISTINCT qal.accessedByUser.id) FROM QuestionAccessLog qal WHERE " +
            "qal.question.id = :questionId")
    Long countUniqueUsersWhoAccessed(@Param("questionId") Long questionId);
}
