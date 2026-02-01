package com.my.challenger.service;

import com.my.challenger.dto.quiz.AiValidationResult;

/**
 * Service for AI-powered answer validation
 */
public interface AiAnswerValidationService {
    /**
     * Validate if a user's answer is semantically equivalent to the correct answer
     * using DeepSeek AI. Returns a result with confidence and explanation.
     * Falls back to local validation if AI is unavailable.
     *
     * @param userAnswer    The user's answer
     * @param correctAnswer The correct answer
     * @param language      The language context (e.g., "en", "ru")
     * @return Validation result
     */
    AiValidationResult validateAnswerWithAi(String userAnswer, String correctAnswer, String language);

    /**
     * Check if the AI validation service is available and configured
     * @return true if available
     */
    boolean isAvailable();
}
