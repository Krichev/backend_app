package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizSessionRequest {
    @NotBlank(message = "Challenge ID is required")
    private Long challengeId;

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String teamName;

    @NotEmpty(message = "At least one team member is required")
    @Size(max = 10, message = "Maximum 10 team members allowed")
    private List<@NotBlank @Size(max = 50) String> teamMembers;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    @Min(value = 10, message = "Round time must be at least 10 seconds")
    @Max(value = 300, message = "Round time must not exceed 300 seconds")
    private int roundTimeSeconds;

    @Min(value = 1, message = "Must have at least 1 round")
    @Max(value = 50, message = "Maximum 50 rounds allowed")
    private int totalRounds;

    @Builder.Default
    private boolean enableAiHost = false;

    @NotBlank(message = "Question source is required")
    @Pattern(regexp = "^(app|user)$", message = "Question source must be 'app' or 'user'")
    private String questionSource;

    // For existing user questions
    @Size(max = 50, message = "Maximum 50 custom questions allowed")
    private List<Long> customQuestionIds;

    // For new custom questions with multimedia support
    @Size(max = 50, message = "Maximum 50 new questions allowed")
    private List<CreateQuestionRequest> newCustomQuestions;

    // For app-generated questions to be saved
    @Size(max = 50, message = "Maximum 50 app questions allowed")
    private List<AppQuestionData> appQuestions;
}