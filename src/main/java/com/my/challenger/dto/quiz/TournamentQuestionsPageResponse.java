package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for tournament questions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionsPageResponse {
    
    private List<TournamentQuestionSummaryDTO> questions;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
    
    // Summary stats
    private Integer totalQuestions;
    private Integer activeQuestions;
    private Integer bonusQuestions;
    private Integer questionsWithMedia;
    private Integer totalPoints;
}