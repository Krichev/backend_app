package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizQuestionRequest {
    @NotBlank(message = "Question text is required")
    @Size(max = 1000, message = "Question text must not exceed 1000 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 500, message = "Answer must not exceed 500 characters")
    private String answer;

    @NotNull(message = "Difficulty is required")
    private QuizDifficulty difficulty;

    @Size(max = 100, message = "Topic must not exceed 100 characters")
    private String topic;

    @Size(max = 500, message = "Additional info must not exceed 500 characters")
    private String additionalInfo;

    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    // New multimedia fields
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    private String questionMediaUrl;

    private String questionMediaId;

    private String questionMediaType;

    private String questionThumbnailUrl;
}