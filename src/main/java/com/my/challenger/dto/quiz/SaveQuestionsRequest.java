package com.my.challenger.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// SaveQuestionsRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveQuestionsRequest {
    @Valid
    @NotEmpty(message = "Questions list cannot be empty")
    private List<CreateQuizQuestionRequest> questions;
}