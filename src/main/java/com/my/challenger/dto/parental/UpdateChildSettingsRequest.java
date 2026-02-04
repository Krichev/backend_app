package com.my.challenger.dto.parental;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UpdateChildSettingsRequest {
    private Integer dailyBudgetMinutes;
    private BigDecimal maxWagerAmount;
    private Boolean allowMoneyWagers;
    private Boolean allowScreenTimeWagers;
    private Boolean allowSocialWagers;
    private Integer maxExtensionRequestsPerDay;
    private List<String> restrictedCategories;
    private String contentAgeRating;
    private Map<String, Boolean> notifications;
}
