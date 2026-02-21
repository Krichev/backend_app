package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleGameDTO {
    private Long id;
    private Long challengeId;
    private String gameMode;
    private int gridRows;
    private int gridCols;
    private int totalPieces;
    private String difficulty;
    private String status;
    private Integer timeLimitSeconds;
    private String hintText;
    private int participantCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}
