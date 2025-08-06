package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



// QuizConfig.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizConfig {
    @NotBlank(message = "Game type is required")
    private String gameType;  // "WWW" for What Where When
    
    @NotBlank(message = "Team name is required")
    private String teamName;
    
    @NotEmpty(message = "Team members are required")
    private List<String> teamMembers;
    
    @NotBlank(message = "Difficulty is required")
    private String difficulty;  // "Easy", "Medium", "Hard"
    
    @Min(value = 10, message = "Round time must be at least 10 seconds")
    @Max(value = 300, message = "Round time cannot exceed 5 minutes")
    private Integer roundTime;
    
    @Min(value = 1, message = "Round count must be at least 1")
    @Max(value = 50, message = "Round count cannot exceed 50")
    private Integer roundCount;
    
    private Boolean enableAIHost = true;
}