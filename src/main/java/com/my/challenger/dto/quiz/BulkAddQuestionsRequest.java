
// src/main/java/com/my/challenger/dto/quiz/BulkAddQuestionsRequest.java
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
 * Request DTO for bulk adding questions to a tournament
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
    
    @NotEmpty(message = "Questions to add list cannot be empty")
    @Valid
    private List<QuestionToAdd> questionsToAdd;
    
    /**
     * Individual question to add
     */
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