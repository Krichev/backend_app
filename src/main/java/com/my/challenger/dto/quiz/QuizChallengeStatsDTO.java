// QuizChallengeStatsDTO.java
package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizChallengeStatsDTO {
    private Long challengeId;
    private Integer totalSessions;
    private Integer completedSessions;
    private Double averageScore;
    private Double averageAccuracy;
    private Long totalQuestions;
}