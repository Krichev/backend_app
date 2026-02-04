package com.my.challenger.dto.parental;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChildScreenTimeDTO {
    private Long childUserId;
    private String childUsername;
    private Integer dailyBudgetMinutes;
    private Integer availableMinutes;
    private Integer lockedMinutes;
    private Integer usedTodayMinutes;
    private boolean isCurrentlyLocked;
}
