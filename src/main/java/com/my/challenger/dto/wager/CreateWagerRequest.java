package com.my.challenger.dto.wager;

import com.my.challenger.entity.enums.CurrencyType;
import com.my.challenger.entity.enums.StakeType;
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
public class CreateWagerRequest {
    private Long challengeId;
    private Long quizSessionId;
    private WagerType wagerType;
    private StakeType stakeType;
    private BigDecimal stakeAmount;
    private CurrencyType stakeCurrency;
    private Integer screenTimeMinutes;
    private String socialPenaltyDescription;
    private List<Long> invitedUserIds;
    private LocalDateTime expiresAt;
    private Integer minParticipants;
    private Integer maxParticipants;
}
