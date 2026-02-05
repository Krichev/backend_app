package com.my.challenger.dto.competitive;

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
public class CompetitiveMatchSummaryDTO {
    private Long id;
    private String matchType;
    private String status;
    private Long player1Id;
    private String player1Username;
    private String player1AvatarUrl;
    private Long player2Id;
    private String player2Username;
    private String player2AvatarUrl;
    private Long winnerId;
    private String winnerUsername;
    private Integer totalRounds;
    private Integer currentRound;
    private BigDecimal player1TotalScore;
    private BigDecimal player2TotalScore;
    private Integer player1RoundsWon;
    private Integer player2RoundsWon;
    private String audioChallengeType;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
