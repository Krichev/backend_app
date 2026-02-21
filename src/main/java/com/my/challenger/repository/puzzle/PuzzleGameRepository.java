package com.my.challenger.repository.puzzle;

import com.my.challenger.entity.enums.PuzzleSessionStatus;
import com.my.challenger.entity.puzzle.PuzzleGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PuzzleGameRepository extends JpaRepository<PuzzleGame, Long> {

    List<PuzzleGame> findByChallengeIdAndStatus(Long challengeId, PuzzleSessionStatus status);

    Optional<PuzzleGame> findByIdAndCreatorId(Long id, Long creatorId);
}
