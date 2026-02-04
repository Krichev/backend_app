package com.my.challenger.repository;

import com.my.challenger.entity.quiz.BrainRingRoundState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrainRingRoundStateRepository extends JpaRepository<BrainRingRoundState, Long> {
    Optional<BrainRingRoundState> findByQuizRoundId(Long quizRoundId);
}
