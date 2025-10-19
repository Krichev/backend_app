package com.my.challenger.dto;

import com.my.challenger.entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Challenge responses with payment and access info
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
    private List<Long> participants;
    private Integer participantCount;
    private String reward;
    private String penalty;
    private String verificationMethod;
    private String targetGroup;
    private FrequencyType frequency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String quizConfig;
    
    // User-specific fields
    private Boolean userIsCreator;
    private String userRole;
    private Boolean userHasJoined;
    private Boolean userHasAccess;
    
    // ========== PAYMENT FIELDS ==========
    
    private PaymentType paymentType;
    private Boolean hasEntryFee;
    private BigDecimal entryFeeAmount;
    private CurrencyType entryFeeCurrency;
    private Boolean hasPrize;
    private BigDecimal prizeAmount;
    private CurrencyType prizeCurrency;
    private BigDecimal prizePool;
    
    // ========== ACCESS CONTROL ==========
    
    private Boolean requiresApproval;
    private Integer invitedUsersCount;
    private Boolean isPublic;
    
    // Convenience methods
    public boolean isFree() {
        return paymentType == PaymentType.FREE || !Boolean.TRUE.equals(hasEntryFee);
    }
    
    public boolean isPaid() {
        return Boolean.TRUE.equals(hasEntryFee) && entryFeeAmount != null && 
               entryFeeAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isPointsBased() {
        return entryFeeCurrency == CurrencyType.POINTS;
    }
    
    public boolean isCashBased() {
        return entryFeeCurrency != CurrencyType.POINTS;
    }
}