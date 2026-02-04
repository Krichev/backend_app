package com.my.challenger.scheduler;

import com.my.challenger.service.ScreenTimeBudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScreenTimeBudgetScheduler {

    private final ScreenTimeBudgetService screenTimeBudgetService;

    /**
     * Resets daily budgets at midnight UTC
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyBudgets() {
        log.info("Starting scheduled daily reset of screen time budgets");
        try {
            screenTimeBudgetService.resetDailyBudgets();
            log.info("Finished scheduled daily reset of screen time budgets");
        } catch (Exception e) {
            log.error("Failed to reset daily budgets", e);
        }
    }
}
