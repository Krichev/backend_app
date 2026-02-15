package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRoundDTO {
    private Long id;
    private Long quizSessionId;
    private QuizQuestionDTO question;
    private Integer roundNumber;
    private String teamAnswer;
    private Boolean isCorrect;
    private String playerWhoAnswered;
    private String discussionNotes;
    private LocalDateTime roundStartedAt;
    private LocalDateTime answerSubmittedAt;
    private Integer discussionDurationSeconds;
    private Integer totalRoundDurationSeconds;
    private Boolean hintUsed;
    
    private Boolean aiValidationUsed;
    private Boolean aiAccepted;
    private BigDecimal aiConfidence;
    private String aiExplanation;

    private LocalDateTime createdAt;
    private Boolean voiceRecordingUsed;
    private String aiFeedback;
}
