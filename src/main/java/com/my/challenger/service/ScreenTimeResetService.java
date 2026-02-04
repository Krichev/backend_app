package com.my.challenger.service;

import java.time.LocalDate;
import java.time.ZoneId;

public interface ScreenTimeResetService {
    
    /**
     * Reset budgets for all users in a specific timezone
     * @return number of users reset
     */
    int resetBudgetsForTimezone(String timezone, LocalDate resetDate);
    
    /**
     * Reset all budgets that are overdue (catch-up reset)
     * @return total users reset across all timezones
     */
    int resetAllOverdueBudgets();
    
    /**
     * Check if a timezone is currently at or past midnight
     */
    boolean isTimezoneAtMidnight(String timezone);
    
    /**
     * Get all timezones that are currently at midnight (within the hour)
     */
    java.util.List<String> getTimezonesAtMidnight();
}
