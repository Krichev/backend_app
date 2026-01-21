package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.QuestionVisibility;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionType;
import com.my.challenger.entity.enums.AudioChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDTO {

    // Basic identifiers
    private Long id;
    private String externalId;
    private Integer legacyQuestionId;

    // Core question content
    private String question;
    private String answer;

    // Classification
    private QuizDifficulty difficulty;
    private QuestionType questionType;
    private String topic;
    private String source;

    // Enhanced metadata
    private String authors;
    private String comments;
    private String passCriteria;
    private String additionalInfo;

    // Media properties
    private String questionMediaUrl;
    private Long questionMediaId;
    private MediaType questionMediaType;
    private String questionThumbnailUrl;

    // Audio challenge fields
    private AudioChallengeType audioChallengeType;
    private Long audioReferenceMediaId;
    private Double audioSegmentStart;
    private Double audioSegmentEnd;
    private Integer minimumScorePercentage;
    private Integer rhythmBpm;
    private String rhythmTimeSignature;
    private String audioChallengeConfig;

    // User creation tracking
    private Boolean isUserCreated;
    private Long creatorId;
    private String creatorUsername;

    // NEW: Access control
    private QuestionVisibility visibility;
    private Long originalQuizId;
    private String originalQuizTitle;

    // NEW: Access information for current user
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canUseInQuiz;

    // Status and usage
    private Boolean isActive;
    private Integer usageCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}