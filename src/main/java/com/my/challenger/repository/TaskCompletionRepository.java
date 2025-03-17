package com.my.challenger.repository;

import com.my.challenger.entity.TaskCompletion;
import com.my.challenger.entity.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {

    List<TaskCompletion> findAllByTaskIdAndUserIdOrderByCompletionDateDesc(Long taskId, Long userId);

    Optional<TaskCompletion> findFirstByTaskIdAndUserIdAndStatusOrderByCompletionDateDesc(Long taskId, Long userId, CompletionStatus status);
}
