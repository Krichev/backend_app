package com.my.challenger.dto.competitive;

import com.my.challenger.dto.audio.QuestionResponseDTO;
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
public class CompetitiveMatchRoundDTO {
    private Long id;
    private Long matchId;
    private Integer roundNumber;
    private String status;
    private QuestionResponseDTO question;
    
    // Player 1
    private BigDecimal player1Score;
    private BigDecimal player1PitchScore;
    private BigDecimal player1RhythmScore;
    private BigDecimal player1VoiceScore;
    private Boolean player1Submitted;
    private LocalDateTime player1SubmittedAt;
    
    // Player 2
    private BigDecimal player2Score;
    private BigDecimal player2PitchScore;
    private BigDecimal player2RhythmScore;
    private BigDecimal player2VoiceScore;
    private Boolean player2Submitted;
    private LocalDateTime player2SubmittedAt;
    
    private Long winnerId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
