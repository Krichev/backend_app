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
 * Complete tournament question with all details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionDetailDTO {
    
    // Tournament Question info
    private Integer id;
    private Integer tournamentId;
    private String tournamentTitle;
    private Integer displayOrder;
    private Integer legacyQuestionNum;
    
    // Quiz Question reference
    private Long quizQuestionId;
    
    // Effective content (considers overrides)
    private String effectiveQuestion;
    private String effectiveAnswer;
    private String effectiveSources;
    
    // From QuizQuestion bank
    private QuizQuestion bankQuestion;
    
    // Tournament-specific overrides
    private String customQuestion;
    private String customAnswer;
    private String customSources;
    
    // Tournament metadata
    private String tournamentType;
    private Integer topicNum;
    private String notices;
    private String images;
    private Integer rating;
    
    // Tournament settings
    private Integer points;
    private Integer timeLimitSeconds;
    private Boolean isBonusQuestion;
    private Boolean isMandatory;
    private Boolean isActive;
    
    // Flags
    private Boolean hasCustomQuestion;
    private Boolean hasCustomAnswer;
    private Boolean hasCustomSources;
    private Boolean hasAnyCustomizations;
    private Boolean hasMedia;
    
    // Audit
    private LocalDateTime enteredDate;
    private LocalDateTime updatedAt;
    private Long addedBy;
    
    /**
     * Nested QuizQuestion info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {
        private Long id;
        private String question;
        private String answer;
        private QuizDifficulty difficulty;
        private String topic;
        private String source;
        private String authors;
        private String comments;
        private String passCriteria;
        private String additionalInfo;
        private QuestionType questionType;
        private String questionMediaUrl;
        private String questionMediaId;
        private MediaType questionMediaType;
        private String questionThumbnailUrl;
        private Integer usageCount;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}