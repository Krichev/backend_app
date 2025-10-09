// src/main/java/com/my/challenger/dto/quiz/AddQuestionToTournamentRequest.java
package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a question to a tournament
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
    
    @Min(value = 0, message = "Points must be non-negative")
    private Integer points; // Optional, defaults to 10 if null
}