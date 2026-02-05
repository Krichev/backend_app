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
public class InvitationSummaryDTO {
    private Long id;
    private Long questId;
    private String questTitle;
    
    private String otherPartyUsername; // inviter or invitee depending on context
    
    private String stakeType;
    private BigDecimal stakeAmount;
    
    private QuestInvitationStatus status;
    private LocalDateTime expiresAt;
    private boolean hasActiveNegotiation;
}
