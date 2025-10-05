package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add existing quiz question to tournament
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddQuestionToTournamentRequest {
    
    @NotNull(message = "Tournament ID is required")
    private Integer tournamentId;
    
    @NotNull(message = "Tournament title is required")
    private String tournamentTitle;
    
    @NotNull(message = "Quiz question ID is required")
    private Long quizQuestionId;
    
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;
    
    private Integer timeLimitSeconds;
    
    private Boolean isBonusQuestion;
    
    private Boolean isMandatory;
    
    // Optional: specify position (null = append to end)
    private Integer insertAtPosition;
    
    // Optional: tournament-specific customizations
    private String customQuestion;
    private String customAnswer;
    private String customSources;
    private String notices;
}