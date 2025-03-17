package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for verification history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationHistoryDTO {
    private Long challengeId;
    private Long userId;
    private String challengeTitle;
    private String userName;
    private String completionDate;
    private String verificationDate;
    private String status;
    private String notes;
    private Map<String, Object> details;
}
