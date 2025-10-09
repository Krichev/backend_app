package com.my.challenger.dto.quiz;

// src/main/java/com/my/challenger/dto/quiz/QuestionAvailabilityDTO.java

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for question availability information
 * Used to check if enough questions are available for a game
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAvailabilityDTO {
    
    private Integer tournamentId;
    private String tournamentTitle;
    private Map<QuizDifficulty, Long> availableByDifficulty;
    private Long totalAvailable;
    private Boolean hasEnoughForGame;
    private Integer minimumRequired;
}