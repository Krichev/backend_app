// src/main/java/com/my/challenger/dto/quiz/CreateQuestionRequest.java
package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CreateQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    private String topic;
    private String additionalInfo;
}