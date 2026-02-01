package com.my.challenger.dto.penalty;

import com.my.challenger.entity.enums.PenaltyStatus;
import com.my.challenger.entity.enums.PenaltyType;
import com.my.challenger.entity.enums.PenaltyVerificationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyDTO {
    private Long id;
    private Long wagerId;
    private Long challengeId;
    private Long assignedToUserId;
    private String assignedToUsername;
    private Long assignedByUserId;
    private String assignedByUsername;
    private PenaltyType penaltyType;
    private String description;
    private PenaltyStatus status;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private PenaltyVerificationMethod verificationMethod;
    private Long verifiedByUserId;
    private LocalDateTime verifiedAt;
    private String proofDescription;
    private String proofMediaUrl;
    private Integer screenTimeMinutes;
    private Long pointAmount;
    private String appealReason;
    private LocalDateTime appealedAt;
    private Boolean escalationApplied;
    private LocalDateTime createdAt;
}
