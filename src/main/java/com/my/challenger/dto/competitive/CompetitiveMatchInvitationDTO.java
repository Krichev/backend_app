package com.my.challenger.dto.competitive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveMatchInvitationDTO {
    private Long id;
    private Long matchId;
    private Long inviterId;
    private String inviterUsername;
    private String inviterAvatarUrl;
    private Long inviteeId;
    private String status;
    private String message;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    // Match details
    private Integer totalRounds;
    private String audioChallengeType;
}
