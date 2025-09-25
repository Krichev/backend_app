package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatsDTO {
    private int totalSessions;
    private int totalScore;
    private double averageScore;
    private double completionRate;
}