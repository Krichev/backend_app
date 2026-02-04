package com.my.challenger.scheduler;

import com.my.challenger.service.ScreenTimeResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScreenTimeBudgetResetScheduler {

    private final ScreenTimeResetService resetService;

    /**
     * Hourly job - checks for timezones hitting midnight and resets them.
     * Runs at the start of every hour.
     * 
     * Lock: 25 minutes max (allows next run at minute 30 if needed)
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @SchedulerLock(
        name = "screenTimeHourlyReset",
        lockAtLeastFor = "PT5M",  // Hold lock for at least 5 minutes
        lockAtMostFor = "PT25M"   // Release after 25 minutes max
    )
    public void hourlyTimezoneReset() {
        log.info("Starting hourly screen time reset check");
        
        try {
            List<String> timezones = resetService.getTimezonesAtMidnight();
            
            if (timezones.isEmpty()) {
                log.debug("No timezones currently at midnight");
                return;
            }
            
            log.info("Found {} timezones at midnight: {}", timezones.size(), timezones);
            
            int totalReset = 0;
            for (String timezone : timezones) {
                try {
                    LocalDate today = LocalDate.now(ZoneId.of(timezone));
                    int reset = resetService.resetBudgetsForTimezone(timezone, today);
                    totalReset += reset;
                } catch (Exception e) {
                    log.error("Failed to reset timezone {}: {}", timezone, e.getMessage());
                }
            }
            
            log.info("Hourly reset complete: {} users reset across {} timezones", 
                    totalReset, timezones.size());
                    
        } catch (Exception e) {
            log.error("Hourly reset job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Daily catch-up job - runs at 00:05 UTC to catch any missed resets.
     * Handles users who fell through the cracks (new users, timezone changes, etc.)
     */
    @Scheduled(cron = "0 5 0 * * *") // 00:05 UTC daily
    @SchedulerLock(
        name = "screenTimeDailyCatchup",
        lockAtLeastFor = "PT10M",
        lockAtMostFor = "PT55M"  // Allow up to 55 minutes for large user bases
    )
    public void dailyCatchupReset() {
        log.info("Starting daily catch-up screen time reset");
        
        try {
            int totalReset = resetService.resetAllOverdueBudgets();
            log.info("Daily catch-up reset complete: {} users reset", totalReset);
        } catch (Exception e) {
            log.error("Daily catch-up reset failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing/admin purposes.
     * Can be called via actuator or admin endpoint.
     */
    public int triggerManualReset(String timezone) {
        log.info("Manual reset triggered for timezone: {}", timezone);
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        return resetService.resetBudgetsForTimezone(timezone, today);
    }
}
