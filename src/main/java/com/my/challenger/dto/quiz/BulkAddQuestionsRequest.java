package com.my.challenger.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to add multiple questions to tournament at once
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddQuestionsRequest {
    
    @NotNull(message = "Tournament ID is required")
    private Integer tournamentId;
    
    @NotNull(message = "Tournament title is required")
    private String tournamentTitle;
    
    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<QuestionToAdd> questions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionToAdd {
        @NotNull(message = "Quiz question ID is required")
        private Long quizQuestionId;
        
        private Integer points;
        private Integer timeLimitSeconds;
        private Boolean isBonusQuestion;
        private String customQuestion;
        private String customAnswer;
    }
}