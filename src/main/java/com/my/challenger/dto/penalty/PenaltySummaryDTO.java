package com.my.challenger.dto.penalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltySummaryDTO {
    private long pendingCount;
    private long inProgressCount;
    private long completedCount;
    private long verifiedCount;
    private long overdueCount;
}
