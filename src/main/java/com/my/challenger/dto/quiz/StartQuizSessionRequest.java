package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StartQuizSessionRequest {
    @NotNull(message = "Challenge ID is required")
    private Long challengeId;

    @NotBlank(message = "Team name is required")
    private String teamName;

    @NotNull(message = "Team members are required")
    private List<String> teamMembers;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    @Min(value = 10, message = "Round time must be at least 10 seconds")
    private Integer roundTimeSeconds = 60;

    @Min(value = 1, message = "Must have at least 1 round")
    private Integer totalRounds = 10;

    private Boolean enableAiHost = false;
    private String questionSource = "app"; // 'app' or 'user'

    // Existing user question support
    private List<Long> customQuestionIds; // For user-created questions

    // Enhanced question support - NEW FIELDS
    private List<CreateQuestionRequest> newCustomQuestions; // For creating new questions on the fly
    private List<AppQuestionData> appQuestions; // For saving app-generated questions
}