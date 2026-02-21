package com.my.challenger.repository.puzzle;

import com.my.challenger.entity.puzzle.PuzzlePiece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuzzlePieceRepository extends JpaRepository<PuzzlePiece, Long> {

    List<PuzzlePiece> findByPuzzleGameIdOrderByPieceIndex(Long puzzleGameId);

    List<PuzzlePiece> findByPuzzleGameIdAndPieceIndexIn(Long puzzleGameId, List<Integer> pieceIndexes);
}
