package com.my.challenger.dto.vibration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {
    private boolean isCorrect;
    private String correctAnswer;
    private Integer pointsEarned;
    private Integer totalScore;
    private Integer correctAnswers;
    private Integer currentQuestionIndex;
    private boolean isGameComplete;
}
