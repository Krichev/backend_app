// UpdatedCreateQuizQuestionRequest.java
package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.MediaSourceType;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    private QuizDifficulty difficulty;
    private String topic;
    private String source;
    private String additionalInfo;
    // Media support fields
    private Long mediaFileId;
    private String questionMediaUrl;
    private Long questionMediaId;
    private MediaType questionMediaType;

    // External Media Support
    private MediaSourceType mediaSourceType;
    private String externalMediaUrl;
    private Double questionVideoStartTime;
    private Double questionVideoEndTime;
    private String answerMediaUrl;
    private Double answerVideoStartTime;
    private Double answerVideoEndTime;
    private String answerTextVerification;

    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @NotNull(message = "Visibility is required")
    @Builder.Default
    private QuestionVisibility visibility = QuestionVisibility.PRIVATE;

    /**
     * For QUIZ_ONLY visibility, specify the quiz/challenge ID
     */
    private Long originalQuizId;
}