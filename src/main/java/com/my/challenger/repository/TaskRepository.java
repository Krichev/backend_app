package com.my.challenger.repository;

import com.my.challenger.entity.Task;
import com.my.challenger.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find tasks by challenge ID
     */
    List<Task> findByChallengeId(Long challengeId);

    /**
     * Find tasks by challenge ID and assigned user
     */
    List<Task> findByChallengeIdAndAssignedTo(Long challengeId, Long userId);

    /**
     * Find the first task with specific challenge and assigned user
     */
    Optional<Task> findFirstByChallengeIdAndAssignedTo(Long challengeId, Long userId);

    /**
     * Find tasks by challenge ID, assigned user, and status
     */
    List<Task> findByChallengeIdAndAssignedToAndStatus(Long challengeId, Long userId, TaskStatus status);

    /**
     * Find the first task with specific challenge, user and status
     */
    Optional<Task> findFirstByChallengeIdAndAssignedToAndStatus(Long challengeId, Long userId, TaskStatus status);

    /**
     * Count tasks by challenge ID and assigned user
     */
    long countByChallengeIdAndAssignedTo(Long challengeId, Long userId);

    /**
     * Count tasks by challenge ID, assigned user, and status
     */
    long countByChallengeIdAndAssignedToAndStatus(Long challengeId, Long userId, TaskStatus status);

    /**
     * Find all tasks assigned to a user
     */
    List<Task> findByAssignedTo(Long userId);

    /**
     * Find all active tasks assigned to a user
     */
    List<Task> findByAssignedToAndStatus(Long userId, TaskStatus status);

    /**
     * Delete all tasks associated with a challenge
     */
    @Modifying
    @Query("DELETE FROM Task t WHERE t.challenge.id = :challengeId")
    void deleteAllByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * Find overdue tasks
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS' AND t.endDate < CURRENT_TIMESTAMP")
    List<Task> findOverdueTasks();

    /**
     * Find tasks due today
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'IN_PROGRESS' AND " +
            "FUNCTION('DATE', t.endDate) = FUNCTION('DATE', CURRENT_TIMESTAMP)")
    List<Task> findTasksDueToday();
}
