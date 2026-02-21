package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzleGameStatusDTO {
    private PuzzleGameDTO game;
    private List<PuzzleParticipantDTO> participants;
    private boolean isStarted;
    private boolean isCompleted;
    private long elapsedTimeMs;     // Time since game started
}
