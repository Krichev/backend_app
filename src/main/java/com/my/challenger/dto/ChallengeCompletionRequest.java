package com.my.challenger.dto;

import lombok.Data;

import java.util.Map;

/**
 * Request DTO for submitting challenge completion
 */
@Data
public class ChallengeCompletionRequest {
    private Map<String, Object> verificationData;
    private String notes;
}
