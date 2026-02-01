// src/main/java/com/my/challenger/service/WWWGameService.java
package com.my.challenger.service;

import com.my.challenger.entity.quiz.QuizRound;

/**
 * Service interface for WWW Game logic operations
 */
public interface WWWGameService {
    
    /**
     * Validate if a team answer is correct
     * @param teamAnswer The answer provided by the team
     * @param correctAnswer The correct answer
     * @return true if the answer is correct (with fuzzy matching)
     */
    boolean validateAnswer(String teamAnswer, String correctAnswer);
    
    /**
     * Generate AI feedback for a round
     * @param round The quiz round
     * @param isCorrect Whether the answer was correct
     * @return AI-generated feedback string
     */
    String generateRoundFeedback(QuizRound round, boolean isCorrect);
    
    /**
     * Generate a hint for a question
     * @param correctAnswer The correct answer
     * @param difficulty The difficulty level
     * @return A hint string
     */
    String generateHint(String correctAnswer, String difficulty);
    
    /**
     * Calculate similarity between two answers (for fuzzy matching)
     * @param answer1 First answer
     * @param answer2 Second answer
     * @return Similarity score between 0.0 and 1.0
     */
    double calculateAnswerSimilarity(String answer1, String answer2);

    /**
     * Validate answer with optional AI-powered semantic matching
     * @param teamAnswer The answer provided by the team
     * @param correctAnswer The correct answer
     * @param enableAiValidation Whether to use AI for semantic matching
     * @param language Language context for better matching (en/ru)
     * @return ValidationResult with isCorrect, aiUsed, explanation
     */
    com.my.challenger.dto.quiz.AnswerValidationResult validateAnswerEnhanced(String teamAnswer, String correctAnswer, 
                                                boolean enableAiValidation, String language);
}


