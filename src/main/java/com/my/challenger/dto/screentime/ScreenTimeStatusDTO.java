package com.my.challenger.dto.screentime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTimeStatusDTO {
    private boolean isLocked;
    private Integer availableMinutes;
    private String lockExpiresAt;
}
