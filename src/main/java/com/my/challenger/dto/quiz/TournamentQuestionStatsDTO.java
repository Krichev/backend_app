package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Statistics about tournament questions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionStatsDTO {
    
    private Integer tournamentId;
    private String tournamentTitle;
    
    // Counts
    private Integer totalQuestions;
    private Integer activeQuestions;
    private Integer inactiveQuestions;
    private Integer bonusQuestions;
    private Integer mandatoryQuestions;
    private Integer questionsWithCustomizations;
    private Integer questionsWithMedia;
    
    // Points
    private Integer totalPoints;
    private Double averagePoints;
    private Integer minPoints;
    private Integer maxPoints;
    
    // Difficulty distribution
    private Map<QuizDifficulty, Integer> difficultyDistribution;
    
    // Question type distribution
    private Map<QuestionType, Integer> questionTypeDistribution;
    
    // Topic distribution
    private Map<String, Integer> topicDistribution;
    
    // Ratings
    private Double averageRating;
    private Integer questionsWithRating;
}