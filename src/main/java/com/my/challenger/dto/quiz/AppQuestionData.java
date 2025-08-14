// src/main/java/com/my/challenger/dto/quiz/AppQuestionData.java
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
public class AppQuestionData {
    @NotBlank(message = "Question text is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    private String topic;
    private String additionalInfo;
    private String externalId; // For tracking original question ID
    private String source; // Source identifier (e.g., "APP_GENERATED")
}