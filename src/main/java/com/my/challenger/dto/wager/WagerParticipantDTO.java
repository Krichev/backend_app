package com.my.challenger.dto.wager;

import com.my.challenger.entity.enums.ParticipantWagerStatus;
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
public class WagerParticipantDTO {
    private Long id;
    private Long userId;
    private String username;
    private ParticipantWagerStatus status;
    private boolean stakeEscrowed;
    private BigDecimal amountWon;
    private BigDecimal amountLost;
    private Integer quizScore;
    private LocalDateTime joinedAt;
    private LocalDateTime settledAt;
}
