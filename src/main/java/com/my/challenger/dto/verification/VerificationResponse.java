package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for verification response (used by both photo and location verification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private boolean success;
    private boolean isVerified;
    private String message;
    private Map<String, Object> details;
}
