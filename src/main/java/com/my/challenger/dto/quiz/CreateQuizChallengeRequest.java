package com.my.challenger.dto.quiz;


import com.my.challenger.entity.enums.FrequencyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizChallengeRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Visibility is required")
    private String visibility; // "PUBLIC" or "PRIVATE"

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ChallengeFrequency frequency;

    private QuizChallengeConfig quizConfig;
    private List<CreateQuizQuestionRequest> customQuestions;
}