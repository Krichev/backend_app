package com.my.challenger.dto;

import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.FrequencyType;
import com.my.challenger.entity.enums.VisibilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Challenge entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDTO {
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
    private String reward;
    private String penalty;
    private String verificationMethod; // JSON string of verification methods
    private String targetGroup;
    private FrequencyType frequency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer participantCount;
    private Boolean userIsCreator; // Flag to indicate if the current user is the creator
    private String userRole;
    private Boolean userHasJoined; // Flag to indicate if the requesting user has joined
    private List<String> tags;

    private String quizConfig; // JSON string of quiz configuration
}