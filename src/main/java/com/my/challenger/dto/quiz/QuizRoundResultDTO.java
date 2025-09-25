package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRoundResultDTO {
    private Long roundId;
    private boolean isCorrect;
    private String correctAnswer;
    private String feedback;
    private Integer sessionScore;
    private boolean isSessionComplete;
}
