package com.my.challenger.repository;

import com.my.challenger.entity.TaskCompletion;
import com.my.challenger.entity.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TaskCompletion entity
 */
@Repository
public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {

    /**
     * Find all task completions for a specific task and user
     */
    List<TaskCompletion> findAllByTaskIdAndUserIdOrderByCompletionDateDesc(Long taskId, Long userId);

    /**
     * Find all task completions for a specific task
     */
    List<TaskCompletion> findByTaskId(Long taskId);

    /**
     * Find all task completions for a specific user
     */
    List<TaskCompletion> findByUserId(Long userId);

    /**
     * Find completions for a task by a specific user
     */
    List<TaskCompletion> findByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * Find all completions for a challenge by a specific user
     */
    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task.challenge.id = :challengeId AND tc.user.id = :userId")
    List<TaskCompletion> findByTaskChallengeIdAndUserId(@Param("challengeId") Long challengeId, @Param("userId") Long userId);

    /**
     * Find all completions for a challenge by a specific user, ordered by completion date
     */
    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task.challenge.id = :challengeId AND tc.user.id = :userId " +
            "ORDER BY tc.completionDate DESC")
    List<TaskCompletion> findByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(
            @Param("challengeId") Long challengeId, @Param("userId") Long userId);

    /**
     * Find the most recent completion for a task by a specific user
     */
    Optional<TaskCompletion> findFirstByTaskIdAndUserIdOrderByCompletionDateDesc(Long taskId, Long userId);

    /**
     * Find the most recent completion for a challenge by a specific user
     */
    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task.challenge.id = :challengeId AND tc.user.id = :userId " +
            "ORDER BY tc.completionDate DESC")
    Optional<TaskCompletion> findFirstByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(
            @Param("challengeId") Long challengeId, @Param("userId") Long userId);

    /**
     * Find the most recent completion with a specific status for a task by a user
     */
    Optional<TaskCompletion> findFirstByTaskIdAndUserIdAndStatusOrderByCompletionDateDesc(
            Long taskId, Long userId, CompletionStatus status);

    /**
     * Count verified completions for a specific user
     */
    long countByUserIdAndStatus(Long userId, CompletionStatus status);

    /**
     * Count all completions by status
     */
    long countByStatus(CompletionStatus status);

    /**
     * Find completions pending verification
     */
    List<TaskCompletion> findByStatus(CompletionStatus status);

    /**
     * Find completions pending verification for a specific challenge
     */
    @Query("SELECT tc FROM TaskCompletion tc WHERE tc.task.challenge.id = :challengeId AND tc.status = :status")
    List<TaskCompletion> findByTaskChallengeIdAndStatus(
            @Param("challengeId") Long challengeId, @Param("status") CompletionStatus status);
}
