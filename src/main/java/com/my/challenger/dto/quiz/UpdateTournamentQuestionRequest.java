package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update tournament-specific question settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTournamentQuestionRequest {
    
    // Tournament-specific overrides
    private String customQuestion;
    private String customAnswer;
    private String customSources;
    
    // Tournament settings
    @Min(value = 0, message = "Points cannot be negative")
    private Integer points;
    
    @Min(value = 0, message = "Time limit cannot be negative")
    private Integer timeLimitSeconds;
    
    private Boolean isBonusQuestion;
    private Boolean isMandatory;
    
    // Metadata
    private String notices;
    private Integer rating;
    
    // Clear overrides flags (if true, clears the corresponding field)
    private Boolean clearCustomQuestion;
    private Boolean clearCustomAnswer;
    private Boolean clearCustomSources;
}