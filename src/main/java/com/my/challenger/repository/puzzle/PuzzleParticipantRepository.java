package com.my.challenger.repository.puzzle;

import com.my.challenger.entity.puzzle.PuzzleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PuzzleParticipantRepository extends JpaRepository<PuzzleParticipant, Long> {

    Optional<PuzzleParticipant> findByPuzzleGameIdAndUserId(Long puzzleGameId, Long userId);

    List<PuzzleParticipant> findByPuzzleGameIdOrderByScoreDesc(Long puzzleGameId);

    long countByPuzzleGameId(Long puzzleGameId);
}
