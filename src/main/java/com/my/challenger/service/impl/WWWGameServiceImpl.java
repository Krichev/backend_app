package com.my.challenger.service.impl;

import com.my.challenger.entity.quiz.QuizRound;
import com.my.challenger.service.WWWGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WWWGameServiceImpl implements WWWGameService {

    @Override
    public boolean validateAnswer(String teamAnswer, String correctAnswer) {
        if (teamAnswer == null || correctAnswer == null) {
            return false;
        }

        // Normalize strings for comparison
        String normalizedTeamAnswer = normalizeForComparison(teamAnswer);
        String normalizedCorrectAnswer = normalizeForComparison(correctAnswer);

        // Direct match
        if (normalizedTeamAnswer.equals(normalizedCorrectAnswer)) {
            return true;
        }

        // Check if team answer contains correct answer or vice versa
        if (normalizedTeamAnswer.contains(normalizedCorrectAnswer) ||
                normalizedCorrectAnswer.contains(normalizedTeamAnswer)) {
            return true;
        }

        // For short answers, use fuzzy matching
        if (normalizedCorrectAnswer.split("\\s+").length <= 2) {
            return isApproximatelyCorrect(normalizedTeamAnswer, normalizedCorrectAnswer);
        }

        // For longer answers, check word overlap
        return hasSignificantWordOverlap(normalizedTeamAnswer, normalizedCorrectAnswer);
    }

    @Override
    public String generateRoundFeedback(QuizRound round, boolean isCorrect) {
        if (isCorrect) {
            return String.format("Correct! '%s' is indeed the right answer. Well done!",
                    round.getQuestion().getAnswer());
        } else {
            // Check if correct answer was mentioned in discussion
            if (round.getDiscussionNotes() != null &&
                    round.getDiscussionNotes().toLowerCase().contains(
                            round.getQuestion().getAnswer().toLowerCase())) {
                return String.format("The correct answer was '%s'. I noticed this was mentioned during your discussion, but it wasn't your final answer.",
                        round.getQuestion().getAnswer());
            } else {
                return String.format("The correct answer was '%s'. Your answer '%s' was not quite right.",
                        round.getQuestion().getAnswer(), round.getTeamAnswer());
            }
        }
    }

    @Override
    public String generateHint(String correctAnswer, String difficulty) {
        if (correctAnswer == null || correctAnswer.isEmpty()) {
            return "No hint available for this question.";
        }

        String hint = "";

        switch (difficulty.toLowerCase()) {
            case "easy":
                // Give first letters and word count
                String[] words = correctAnswer.split("\\s+");
                String firstLetters = Arrays.stream(words)
                        .map(word -> word.substring(0, 1).toUpperCase())
                        .collect(Collectors.joining(" "));
                hint = String.format("The answer starts with: %s (%d word%s)",
                        firstLetters, words.length, words.length != 1 ? "s" : "");
                break;

            case "medium":
                // Give character count and word count
                hint = String.format("The answer has %d characters and %d word%s",
                        correctAnswer.length(),
                        correctAnswer.split("\\s+").length,
                        correctAnswer.split("\\s+").length != 1 ? "s" : "");
                break;

            case "hard":
                // Give only basic structure
                int wordCount = correctAnswer.split("\\s+").length;
                if (wordCount > 1) {
                    hint = String.format("The answer is a %d-word term", wordCount);
                } else {
                    hint = String.format("The answer is a single word with %d letters", correctAnswer.length());
                }
                break;

            default:
                hint = String.format("The answer contains %d characters", correctAnswer.length());
        }

        return hint;
    }

    @Override
    public double calculateAnswerSimilarity(String answer1, String answer2) {
        if (answer1 == null || answer2 == null) {
            return 0.0;
        }

        String norm1 = normalizeForComparison(answer1);
        String norm2 = normalizeForComparison(answer2);

        if (norm1.equals(norm2)) {
            return 1.0;
        }

        // Use Levenshtein distance for similarity
        int distance = levenshteinDistance(norm1, norm2);
        int maxLength = Math.max(norm1.length(), norm2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - (double) distance / maxLength;
    }

    // Helper methods
    private String normalizeForComparison(String text) {
        return text.toLowerCase()
                .trim()
                .replaceAll("[.,\\/#!$%\\^&\\*;:{}=\\-_`~()\"\"''«»]", "")
                .replaceAll("\\s{2,}", " ");
    }

    private boolean isApproximatelyCorrect(String userAnswer, String correctAnswer) {
        double similarity = calculateAnswerSimilarity(userAnswer, correctAnswer);
        return similarity >= 0.8; // 80% similarity threshold
    }

    private boolean hasSignificantWordOverlap(String answer1, String answer2) {
        Set<String> words1 = Arrays.stream(answer1.split("\\s+"))
                .filter(word -> word.length() > 2) // Ignore very short words
                .collect(Collectors.toSet());

        Set<String> words2 = Arrays.stream(answer2.split("\\s+"))
                .filter(word -> word.length() > 2)
                .collect(Collectors.toSet());

        if (words1.isEmpty() || words2.isEmpty()) {
            return false;
        }

        // Calculate intersection
        Set<String> intersection = words1.stream()
                .filter(words2::contains)
                .collect(Collectors.toSet());

        // Require at least 50% word overlap
        double overlapRatio = (double) intersection.size() / Math.min(words1.size(), words2.size());
        return overlapRatio >= 0.5;
    }

    private int levenshteinDistance(String a, String b) {
        if (a.length() == 0) return b.length();
        if (b.length() == 0) return a.length();

        int[][] matrix = new int[b.length() + 1][a.length() + 1];

        for (int i = 0; i <= b.length(); i++) {
            matrix[i][0] = i;
        }

        for (int j = 0; j <= a.length(); j++) {
            matrix[0][j] = j;
        }

        for (int i = 1; i <= b.length(); i++) {
            for (int j = 1; j <= a.length(); j++) {
                if (b.charAt(i - 1) == a.charAt(j - 1)) {
                    matrix[i][j] = matrix[i - 1][j - 1];
                } else {
                    matrix[i][j] = Math.min(
                            matrix[i - 1][j - 1] + 1, // substitution
                            Math.min(
                                    matrix[i][j - 1] + 1,     // insertion
                                    matrix[i - 1][j] + 1      // deletion
                            )
                    );
                }
            }
        }

        return matrix[b.length()][a.length()];
    }
}
