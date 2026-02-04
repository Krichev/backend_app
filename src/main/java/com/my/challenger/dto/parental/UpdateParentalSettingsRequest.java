package com.my.challenger.dto.parental;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParentalSettingsRequest {
    
    @Pattern(regexp = "^(UNDER_13|13_TO_17|ADULT)$", message = "Invalid age group")
    private String ageGroup;
    
    @Pattern(regexp = "^(STRICT|MODERATE|NONE)$", message = "Invalid restriction level")
    private String contentRestrictionLevel;
    
    private Boolean requireParentApproval;
    
    private List<String> allowedTopicCategories;
    
    private List<String> blockedTopicCategories;
    
    @Min(value = 1, message = "Screen time must be at least 1 minute")
    @Max(value = 1440, message = "Screen time cannot exceed 24 hours")
    private Integer maxDailyScreenTimeMinutes;
    
    @Min(value = 1, message = "Quiz count must be at least 1")
    @Max(value = 100, message = "Quiz count cannot exceed 100")
    private Integer maxDailyQuizCount;
    
    private String newParentPin; // For setting/updating PIN (4-6 digits)
}
