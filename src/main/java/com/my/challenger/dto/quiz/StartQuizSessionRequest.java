package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuestionSource;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizSessionRequest {
    
    @NotNull(message = "Challenge ID is required")
    private Long challengeId;
    
    @NotNull(message = "Team name is required")
    @Size(min = 1, max = 200, message = "Team name must be between 1 and 200 characters")
    private String teamName;
    
    @NotNull(message = "Team members are required")
    @Size(min = 1, message = "At least one team member is required")
    private List<String> teamMembers;
    
    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;
    
    @NotNull(message = "Total rounds is required")
    @Min(value = 1, message = "Must have at least 1 round")
    private Integer totalRounds;
    
    @NotNull(message = "Round time is required")
    @Min(value = 10, message = "Round time must be at least 10 seconds")
    private Integer roundTimeSeconds;
    
    private Boolean enableAiHost;
    
    @NotNull(message = "Question source is required")
    private QuestionSource questionSource; // "app" or "user"
    
    // For user-selected questions
    private List<Long> customQuestionIds;
    private Boolean enableAiAnswerValidation;
}