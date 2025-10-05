package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to search/filter tournament questions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionSearchRequest {
    
    private Integer tournamentId;
    private QuizDifficulty difficulty;
    private String topic;
    private QuestionType questionType;
    private Boolean hasMedia;
    private Boolean isBonusQuestion;
    private Boolean hasCustomizations;
    private Boolean isActive;
    
    // Search in question text
    private String searchText;
    
    // Pagination
    private Integer page;
    private Integer size;
    
    // Sorting
    private String sortBy; // displayOrder, difficulty, rating, etc.
    private String sortDirection; // ASC, DESC
}