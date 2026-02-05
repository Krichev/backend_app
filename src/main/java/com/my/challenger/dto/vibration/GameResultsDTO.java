package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.VibrationDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultsDTO {
    private UUID sessionId;
    private Integer totalScore;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Double accuracyPercent;
    private Double averageResponseTimeMs;
    private Integer fastestResponseMs;
    private Integer totalReplaysUsed;
    private Integer perfectRounds;
    private VibrationDifficulty difficulty;
    private Integer rank;
    private Double percentile;
    private List<RoundResultDTO> rounds;
}
