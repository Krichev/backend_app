package com.my.challenger.dto;

import com.my.challenger.entity.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new challenge
 */
@Data
public class CreateChallengeRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Challenge type is required")
    private ChallengeType type;
    
    @NotNull(message = "Visibility is required")
    private VisibilityType visibility;
    
    private ChallengeStatus status = ChallengeStatus.ACTIVE;
    
    private String reward;
    
    private String penalty;
    
    private VerificationMethod verificationMethod; // JSON string of verification methods
    
    private String targetGroup;
    
    private FrequencyType frequency;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private List<String> tags;
}

