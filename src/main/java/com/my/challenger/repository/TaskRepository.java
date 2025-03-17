package com.my.challenger.repository;

import com.my.challenger.entity.Task;
import com.my.challenger.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findFirstByChallengeIdAndAssignedToAndStatus(Long challengeId, Long userId, TaskStatus type);

}
