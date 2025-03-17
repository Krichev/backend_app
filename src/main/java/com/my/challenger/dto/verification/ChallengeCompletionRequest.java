package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for challenge completion submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCompletionRequest {
    private Long challengeId;
    private Map<String, Object> verificationData;
    private String notes;
}
