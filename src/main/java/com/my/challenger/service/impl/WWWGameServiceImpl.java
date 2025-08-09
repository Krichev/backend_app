package com.my.challenger.service.impl;

import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.service.WWWGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WWWGameServiceImpl implements WWWGameService {

    private static final double SIMILARITY_THRESHOLD = 0.8;

    @Override
    public boolean validateAnswer(String teamAnswer, String correctAnswer) {
        if (teamAnswer == null || correctAnswer == null) {
            return false;
        }

        // Normalize answers for comparison
        String normalizedTeamAnswer = normalizeAnswer(teamAnswer);
        String normalizedCorrectAnswer = normalizeAnswer(correctAnswer);

        // Check exact match first
        if (normalizedTeamAnswer.equals(normalizedCorrectAnswer)) {
            return true;
        }

        // Check similarity for fuzzy matching
        double similarity = calculateAnswerSimilarity(normalizedTeamAnswer, normalizedCorrectAnswer);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    @Override
    public String generateRoundFeedback(QuizRound round, boolean isCorrect) {
        if (round == null) {
            return "Unable to generate feedback for this round.";
        }

        StringBuilder feedback = new StringBuilder();
        
        if (isCorrect) {
            feedback.append("🎉 Excellent! That's correct! ");
            
            // Add encouraging message based on question difficulty
            if (round.getQuestion() != null && round.getQuestion().getDifficulty() != null) {
                switch (round.getQuestion().getDifficulty()) {
                    case HARD:
                        feedback.append("That was a challenging question - well done!");
                        break;
                    case MEDIUM:
                        feedback.append("Great teamwork on that one!");
                        break;
                    case EASY:
                        feedback.append("Nice work getting warmed up!");
                        break;
                }
            }
        } else {
            feedback.append("❌ Not quite right. ");
            feedback.append("The correct answer was: ").append(round.getQuestion().getAnswer()).append(". ");
            
            // Provide learning feedback
            if (round.getTeamAnswer() != null && !round.getTeamAnswer().trim().isEmpty()) {
                double similarity = calculateAnswerSimilarity(
                    normalizeAnswer(round.getTeamAnswer()), 
                    normalizeAnswer(round.getQuestion().getAnswer())
                );
                
                if (similarity > 0.5) {
                    feedback.append("You were close though! ");
                }
            }
            
            feedback.append("Keep going - you've got this!");
        }

        // Add hint usage feedback
        if (round.getHintUsed() != null && round.getHintUsed()) {
            feedback.append(" (Good strategy using the hint!)");
        }

        return feedback.toString();
    }

    @Override
    public String generateHint(String correctAnswer, String difficulty) {
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return "Think about the category of this question.";
        }

        String answer = correctAnswer.trim();
        
        // Generate hint based on difficulty
        switch (difficulty != null ? difficulty.toUpperCase() : "MEDIUM") {
            case "EASY":
                // For easy questions, give the first letter and length
                return String.format("The answer starts with '%s' and has %d letters.", 
                    answer.substring(0, 1).toUpperCase(), answer.length());
                    
            case "HARD":
                // For hard questions, give a more cryptic hint
                if (answer.length() > 5) {
                    return String.format("The answer has %d letters and contains the letter '%s'.", 
                        answer.length(), 
                        answer.substring(answer.length() / 2, answer.length() / 2 + 1).toUpperCase());
                } else {
                    return String.format("The answer has %d letters.", answer.length());
                }
                
            case "MEDIUM":
            default:
                // For medium questions, give first letter and a category hint
                return String.format("The answer starts with '%s' and has %d letters. Think about the topic!", 
                    answer.substring(0, 1).toUpperCase(), answer.length());
        }
    }

    @Override
    public double calculateAnswerSimilarity(String answer1, String answer2) {
        if (answer1 == null || answer2 == null) {
            return 0.0;
        }

        String norm1 = normalizeAnswer(answer1);
        String norm2 = normalizeAnswer(answer2);

        if (norm1.equals(norm2)) {
            return 1.0;
        }

        // Use Levenshtein distance for similarity calculation
        int distance = levenshteinDistance(norm1, norm2);
        int maxLength = Math.max(norm1.length(), norm2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Normalize answer text for comparison
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        
        return answer.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "") // Remove punctuation
                .replaceAll("\\s+", " "); // Normalize whitespace
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}