package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleParticipantDTO {
    private Long userId;
    private String username;
    private int piecesPlacedCorrectly;
    private int totalMoves;
    private boolean answerSubmitted;
    private boolean answerCorrect;
    private int score;
    private Long completionTimeMs;
}
