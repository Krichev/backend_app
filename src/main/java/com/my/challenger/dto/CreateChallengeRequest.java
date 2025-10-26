package com.my.challenger.dto;

import com.my.challenger.entity.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new challenge with payment and access control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private VerificationMethod verificationMethod;

    private Map<String, Object> verificationDetails;

    private String targetGroup;

    private FrequencyType frequency;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> tags;

    // ========== PAYMENT FIELDS ==========
    
    private PaymentType paymentType = PaymentType.FREE;

    private Boolean hasEntryFee = false;

    private BigDecimal entryFeeAmount;

    private CurrencyType entryFeeCurrency;

    private Boolean hasPrize = false;

    private BigDecimal prizeAmount;

    private CurrencyType prizeCurrency;

    // ========== ACCESS CONTROL FIELDS ==========
    
    /**
     * List of user IDs who should have access to this private challenge
     * Only applicable when visibility is PRIVATE
     */
    private List<Long> invitedUserIds;

    /**
     * Whether joining requires approval from creator
     */
    private Boolean requiresApproval = false;

    private Long userId;
}