package com.my.challenger.dto.puzzle;

import com.my.challenger.entity.enums.PuzzleGameMode;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePuzzleGameRequest {
    @NotNull(message = "Challenge ID is required")
    private Long challengeId;

    @NotNull(message = "Source image media ID is required")
    private Long sourceImageMediaId;

    @NotNull(message = "Game mode is required")
    private PuzzleGameMode gameMode;

    @Min(value = 2, message = "Grid rows must be at least 2")
    @Max(value = 8, message = "Grid rows must be at most 8")
    @Builder.Default
    private int gridRows = 3;

    @Min(value = 2, message = "Grid columns must be at least 2")
    @Max(value = 8, message = "Grid columns must be at most 8")
    @Builder.Default
    private int gridCols = 3;

    @NotBlank(message = "Answer is required")
    @Size(max = 500, message = "Answer must be at most 500 characters")
    private String answer;

    private List<String> answerAliases;

    @NotNull(message = "Difficulty is required")
    @Builder.Default
    private QuizDifficulty difficulty = QuizDifficulty.MEDIUM;

    private Integer timeLimitSeconds;

    @Size(max = 500, message = "Hint text must be at most 500 characters")
    private String hintText;

    private Integer hintAvailableAfterSeconds;

    @Builder.Default
    private boolean enableAiValidation = false;
}
