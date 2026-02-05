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
public class MatchResultDTO {
    private Long matchId;
    private Long winnerId;
    private String winnerUsername;
    private Boolean isDraw;
    
    private BigDecimal player1TotalScore;
    private BigDecimal player2TotalScore;
    private Integer player1RoundsWon;
    private Integer player2RoundsWon;
    
    private LocalDateTime completedAt;
    private Integer totalDurationSeconds;
    
    // Wager outcome if applicable
    private BigDecimal amountWon;
    private String currency;
}
