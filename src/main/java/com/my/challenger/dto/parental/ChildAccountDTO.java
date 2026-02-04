package com.my.challenger.dto.parental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildAccountDTO {
    private Long userId;
    private String username;
    private String email;
    private String ageGroup;
    private String contentRestrictionLevel;
    private Boolean requireParentApproval;
    private Integer maxDailyScreenTimeMinutes;
    private Integer maxDailyQuizCount;
    private LocalDateTime linkedAt;
}
