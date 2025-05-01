package com.my.challenger.dto;

import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.FrequencyType;
import com.my.challenger.entity.enums.VisibilityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for updating an existing challenge
 */
@Data
public class UpdateChallengeRequest {
    private String title;

    private String description;

    private ChallengeType type;

    private VisibilityType visibility;

    private ChallengeStatus status;

    private String reward;

    private String penalty;

    private String verificationMethod; // JSON string of verification methods

    private String targetGroup;

    private FrequencyType frequency;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> tags;
}
