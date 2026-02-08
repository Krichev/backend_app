package com.my.challenger.dto.lock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLockConfigDTO {
    private Long userId;
    private Long configuredBy;
    private Boolean allowSelfUnlock;
    private Boolean allowEmergencyBypass;
    private Integer maxEmergencyBypassesPerMonth;
    private BigDecimal unlockPenaltyMultiplier;
    private Long requireApprovalFrom;
    private Boolean escalationEnabled;
    private Integer escalationAfterAttempts;
    private Integer emergencyBypassesUsedThisMonth;
    private Integer emergencyBypassesRemaining;
    private LocalDate bypassMonthResetDate;
}
