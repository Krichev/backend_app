package com.my.challenger.dto.invitation;

import com.my.challenger.entity.enums.NegotiationStatus;
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
public class InvitationNegotiationDTO {
    private Long id;
    private Long invitationId;
    private Long proposerId;
    private String proposerUsername;
    private boolean isProposerInviter;
    
    private String counterStakeType;
    private BigDecimal counterStakeAmount;
    private String counterStakeCurrency;
    private Integer counterScreenTimeMinutes;
    private String counterSocialPenaltyDescription;
    
    private NegotiationStatus status;
    private String message;
    
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
