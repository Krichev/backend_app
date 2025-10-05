package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Basic tournament question info without full quiz question details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionSummaryDTO {
    
    private Integer id;
    private Long quizQuestionId;
    private Integer tournamentId;
    private String tournamentTitle;
    private Integer displayOrder;
    
    // Question preview (first 100 chars)
    private String questionPreview;
    
    // Basic metadata from quiz question
    private QuizDifficulty difficulty;
    private String topic;
    private QuestionType questionType;
    private Boolean hasMedia;
    
    // Tournament-specific
    private Integer points;
    private Boolean isBonusQuestion;
    private Boolean isMandatory;
    private Boolean isActive;
    private Integer rating;
    
    // Flags
    private Boolean hasCustomizations;
    
    // Audit
    private LocalDateTime enteredDate;
    private LocalDateTime updatedAt;
}