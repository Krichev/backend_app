package com.my.challenger.dto.invitation;

import com.my.challenger.entity.enums.QuestInvitationStatus;
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
public class QuestInvitationDTO {
    private Long id;
    private Long questId;
    private String questTitle;
    
    private Long inviterId;
    private String inviterUsername;
    
    private Long inviteeId;
    private String inviteeUsername;
    
    private String stakeType;
    private BigDecimal stakeAmount;
    private String stakeCurrency;
    private Integer screenTimeMinutes;
    private String socialPenaltyDescription;
    
    private QuestInvitationStatus status;
    private String message;
    private LocalDateTime expiresAt;
    
    private InvitationNegotiationDTO currentNegotiation;
    
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    
    // Computed fields
    private boolean isExpired;
    private boolean canNegotiate;
    private Long timeRemainingSeconds;
}
