// src/main/java/com/my/challenger/dto/quiz/ReorderTournamentQuestionsRequest.java
package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for reordering tournament questions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderTournamentQuestionsRequest {
    
    @NotNull(message = "Tournament ID is required")
    private Integer tournamentId;

    @NotEmpty(message = "Question IDs list cannot be empty")
    private List<Integer> questionIds;
}