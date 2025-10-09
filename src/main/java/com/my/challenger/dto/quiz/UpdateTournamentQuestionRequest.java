// src/main/java/com/my/challenger/dto/quiz/UpdateTournamentQuestionRequest.java
package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a tournament question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTournamentQuestionRequest {
    
    private String customQuestion;
    private String customAnswer;
    private String customSources;
    
    @Min(value = 0, message = "Points must be non-negative")
    private Integer points;
    
    @Min(value = 0, message = "Time limit must be non-negative")
    private Integer timeLimitSeconds;
    
    private Boolean isBonusQuestion;
    private Boolean isMandatory;
    private String notices;
    private Integer rating;
    
    // Flags to clear custom fields
    private Boolean clearCustomQuestion;
    private Boolean clearCustomAnswer;
    private Boolean clearCustomSources;
}
