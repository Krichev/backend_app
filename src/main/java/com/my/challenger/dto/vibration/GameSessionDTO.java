package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.entity.enums.VibrationSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionDTO {
    private UUID id;
    private String userId;
    private VibrationDifficulty difficulty;
    private Integer questionCount;
    private Integer maxReplaysPerQuestion;
    private Integer guessTimeLimitSeconds;
    private VibrationSessionStatus status;
    private Integer currentQuestionIndex;
    private Integer totalScore;
    private Integer correctAnswers;
    private List<VibrationSongDTO> questions;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
