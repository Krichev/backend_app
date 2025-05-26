package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateQuizQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    private String topic;
    private String source;
    private String additionalInfo;
}
