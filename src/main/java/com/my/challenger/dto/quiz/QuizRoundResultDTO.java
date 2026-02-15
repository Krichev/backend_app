package com.my.challenger.dto.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRoundResultDTO {
    private Long roundId;
    @JsonProperty("isCorrect")
    private boolean isCorrect;
    private String correctAnswer;
    private String feedback;
    private boolean hintUsed;
    private String hint;

    private boolean aiValidationUsed;
    private boolean aiAccepted;
    private BigDecimal aiConfidence;
    private String aiExplanation;

    private int sessionScore;
    @JsonProperty("isSessionComplete")
    private boolean isSessionComplete;
}
