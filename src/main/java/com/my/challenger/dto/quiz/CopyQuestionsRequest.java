package com.my.challenger.dto.quiz;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to copy questions from one tournament to another
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyQuestionsRequest {
    
    @NotNull(message = "Source tournament ID is required")
    private Integer sourceTournamentId;
    
    @NotNull(message = "Target tournament ID is required")
    private Integer targetTournamentId;
    
    @NotNull(message = "Target tournament title is required")
    private String targetTournamentTitle;
    
    // If true, copy all questions. If false, copy only specified question IDs
    private Boolean copyAll;
    
    // Specific question IDs to copy (if copyAll = false)
    private java.util.List<Integer> questionIds;
    
    // If true, also copy customizations
    private Boolean includeCustomizations;
    
    // If true, preserve display order. If false, append to end
    private Boolean preserveOrder;
}