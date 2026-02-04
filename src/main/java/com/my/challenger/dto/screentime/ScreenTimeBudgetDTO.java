package com.my.challenger.dto.screentime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTimeBudgetDTO {
    private Long id;
    private Long userId;
    private Integer dailyBudgetMinutes;
    private Integer availableMinutes;
    private Integer lockedMinutes;
    private Integer lostMinutes; // lostTodayMinutes
    private Long totalWonMinutes;
    private Long totalLostMinutes;
    private String lastResetDate; // ISO date string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
