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
public class MatchmakingStatusDTO {
    private String status;
    private Integer estimatedWaitSeconds;
    private Integer queuePosition;
    private LocalDateTime queuedAt;
    private String audioChallengeType;
    private Integer preferredRounds;
}
