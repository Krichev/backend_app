package com.my.challenger.dto.quiz;


import com.my.challenger.entity.enums.FrequencyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    
    private String description;
    
    @NotBlank(message = "Type is required")
    private String type;  // Should be "QUIZ"
    
    @NotBlank(message = "Visibility is required")
    private String visibility;  // "PUBLIC" or "PRIVATE"
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private FrequencyType frequency;
    
    @Valid
    private QuizConfig quizConfig;
    
    @Valid
    private List<CreateQuizQuestionRequest> userQuestions;
}