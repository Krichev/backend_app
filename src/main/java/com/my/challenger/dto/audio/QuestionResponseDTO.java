package com.my.challenger.dto.audio;

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for audio challenge questions
 * Contains both standard question fields and audio-specific configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDTO {

    // Basic identifiers
    private Long id;
    private String externalId;

    // Core question content
    private String question;
    private String answer;

    // Classification
    private String questionType;
    private String difficulty;
    private String topic;

    // Media properties
    private String mediaUrl;
    private String questionMediaId;
    private String questionMediaType;

    // Audio challenge specific fields
    private String audioChallengeType;
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

    // Status and metadata
    private Boolean isActive;
    private Integer usageCount;
    private String additionalInfo;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
