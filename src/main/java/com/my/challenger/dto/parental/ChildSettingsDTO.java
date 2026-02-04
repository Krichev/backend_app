package com.my.challenger.dto.parental;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChildSettingsDTO {
    private Long childUserId;
    private Integer dailyBudgetMinutes;
    private BigDecimal maxWagerAmount;
    private boolean allowMoneyWagers;
    private boolean allowScreenTimeWagers;
    private boolean allowSocialWagers;
    private Integer maxExtensionRequestsPerDay;
    private List<String> restrictedCategories;
    private String contentAgeRating;
    private Map<String, Boolean> notifications;
}
