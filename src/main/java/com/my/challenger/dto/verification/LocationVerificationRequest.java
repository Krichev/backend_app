package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for location verification request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationVerificationRequest {
    private Long challengeId;
    private double latitude;
    private double longitude;
    private String timestamp;
    private Map<String, Object> additionalData;
}

