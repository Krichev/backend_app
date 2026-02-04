package com.my.challenger.dto.parental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserParentalSettingsDTO {
    private Long id;
    private Long userId;
    private Boolean isChildAccount;
    private Long parentUserId;
    private String parentUsername; // Resolved from parentUserId
    private String ageGroup;
    private String contentRestrictionLevel;
    private Boolean requireParentApproval;
    private List<String> allowedTopicCategories;
    private List<String> blockedTopicCategories;
    private Integer maxDailyScreenTimeMinutes;
    private Integer maxDailyQuizCount;
    private Boolean hasParentPin;
    private LocalDateTime lastParentVerification;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Integer linkedChildrenCount;
}
