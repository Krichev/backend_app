package com.my.challenger.dto.wager;

import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.entity.enums.StakeType;
import com.my.challenger.entity.enums.WagerStatus;
import com.my.challenger.entity.enums.WagerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WagerDTO {
    private Long id;
    private Long challengeId;
    private Long quizSessionId;
    private Long creatorId;
    private String creatorUsername;
    private WagerType wagerType;
    private StakeType stakeType;
    private BigDecimal stakeAmount;
    private CurrencyType stakeCurrency;
    private WagerStatus status;
    private Integer minParticipants;
    private Integer maxParticipants;
    private String socialPenaltyDescription;
    private Integer screenTimeMinutes;
    private LocalDateTime expiresAt;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private List<WagerParticipantDTO> participants;
}
