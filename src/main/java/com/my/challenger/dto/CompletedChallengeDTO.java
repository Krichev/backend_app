package com.my.challenger.dto;

import com.my.challenger.entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedChallengeDTO {
    private Long id;
    private String title;
    private String description;
    private ChallengeType type;
    private VisibilityType visibility;
    private ChallengeStatus status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Long creator_id;
    private String creatorUsername;
    private String verificationMethod;
    private String quizConfig;
    
    // Additional stats for completed challenges
    private Integer sessionCount;
    private Integer bestScore;
    private Double bestScorePercentage;
    private LocalDateTime lastPlayedAt;
    private Integer totalRounds;
}
