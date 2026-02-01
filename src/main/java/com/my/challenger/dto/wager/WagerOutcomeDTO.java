package com.my.challenger.dto.wager;

import com.my.challenger.entity.enums.SettlementType;
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
public class WagerOutcomeDTO {
    private Long id;
    private Long wagerId;
    private Long winnerId;
    private String winnerUsername;
    private Long loserId;
    private String loserUsername;
    private SettlementType settlementType;
    private BigDecimal amountDistributed;
    private boolean penaltyAssigned;
    private String notes;
    private LocalDateTime settledAt;
}
