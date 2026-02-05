package com.my.challenger.dto.invitation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestInvitationRequest {
    @NotNull
    private Long questId;
    
    @NotNull
    private Long inviteeId;
    
    @NotNull
    private String stakeType;
    
    @Positive
    private BigDecimal stakeAmount;
    
    private String stakeCurrency;
    
    private Integer screenTimeMinutes;
    
    private String socialPenaltyDescription;
    
    @Size(max = 500)
    private String message;
    
    @NotNull
    @Future
    private LocalDateTime expiresAt;
}
