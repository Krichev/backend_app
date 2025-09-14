package com.my.challenger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVerificationDetailsRequest {
    
    @NotNull(message = "Challenge ID is required")
    private Long challengeId;
    
    private String activityType;
    
    @Positive(message = "Target value must be positive")
    private Double targetValue;
    
    @Positive(message = "Radius must be positive")
    private Double radius;
    
    // Location details
    private Double latitude;
    private Double longitude;
    
    // Photo details
    private String description;
    private Boolean requiresPhotoComparison;
    private String verificationMode;
}