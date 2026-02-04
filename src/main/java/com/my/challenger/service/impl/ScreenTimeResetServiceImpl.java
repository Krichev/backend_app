package com.my.challenger.service.impl;

import com.my.challenger.entity.ScreenTimeBudget;
import com.my.challenger.entity.ScreenTimeResetLog;
import com.my.challenger.repository.ScreenTimeBudgetRepository;
import com.my.challenger.repository.ScreenTimeResetLogRepository;
import com.my.challenger.service.ScreenTimeResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenTimeResetServiceImpl implements ScreenTimeResetService {

    private final ScreenTimeBudgetRepository budgetRepository;
    private final ScreenTimeResetLogRepository resetLogRepository;

    @Value("${screen-time.reset.batch-size:500}")
    private int batchSize;

    @Value("${spring.application.name:challenger}")
    private String applicationName;

    @Override
    @Transactional
    public int resetBudgetsForTimezone(String timezone, LocalDate resetDate) {
        log.info("Starting screen time reset for timezone: {} on date: {}", timezone, resetDate);
        
        long startTime = System.currentTimeMillis();
        String instanceId = getInstanceId();
        
        // Create audit log entry
        ScreenTimeResetLog resetLog = ScreenTimeResetLog.builder()
                .resetDate(resetDate)
                .timezone(timezone)
                .startedAt(LocalDateTime.now())
                .status("IN_PROGRESS")
                .instanceId(instanceId)
                .build();
        resetLog = resetLogRepository.save(resetLog);

        int totalReset = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        int totalProcessed = 0;

        try {
            // Count total users to process
            long totalUsers = budgetRepository.countByTimezoneNeedingReset(timezone, resetDate);
            log.info("Found {} users in timezone {} needing reset", totalUsers, timezone);

            // Process in batches
            int page = 0;
            while (true) {
                List<ScreenTimeBudget> batch = budgetRepository.findByTimezoneNeedingReset(
                        timezone, resetDate, PageRequest.of(page, batchSize));

                if (batch.isEmpty()) {
                    break;
                }

                // Process this batch
                BatchResult result = processBatch(batch, resetDate);
                totalReset += result.reset;
                totalFailed += result.failed;
                totalSkipped += result.skipped;
                totalProcessed += batch.size();

                log.debug("Processed batch {}: reset={}, failed={}, skipped={}", 
                        page, result.reset, result.failed, result.skipped);

                page++;
            }

            // Update audit log
            long duration = System.currentTimeMillis() - startTime;
            resetLog.setUsersProcessed(totalProcessed);
            resetLog.setUsersReset(totalReset);
            resetLog.setUsersFailed(totalFailed);
            resetLog.setUsersSkipped(totalSkipped);
            resetLog.setCompletedAt(LocalDateTime.now());
            resetLog.setDurationMs(duration);
            resetLog.setStatus(totalFailed == 0 ? "SUCCESS" : "PARTIAL");
            resetLogRepository.save(resetLog);

            log.info("Completed reset for timezone {}: processed={}, reset={}, failed={}, skipped={}, duration={}ms",
                    timezone, totalProcessed, totalReset, totalFailed, totalSkipped, duration);

            return totalReset;

        } catch (Exception e) {
            log.error("Failed to reset budgets for timezone {}: {}", timezone, e.getMessage(), e);
            
            resetLog.setCompletedAt(LocalDateTime.now());
            resetLog.setStatus("FAILED");
            resetLog.setErrorMessage(e.getMessage());
            resetLog.setDurationMs(System.currentTimeMillis() - startTime);
            resetLogRepository.save(resetLog);

            throw new RuntimeException("Reset failed for timezone: " + timezone, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected BatchResult processBatch(List<ScreenTimeBudget> batch, LocalDate resetDate) {
        int reset = 0;
        int failed = 0;
        int skipped = 0;

        List<Long> idsToReset = new ArrayList<>();

        for (ScreenTimeBudget budget : batch) {
            try {
                // Skip if already reset today
                if (budget.getLastResetDate() != null && 
                    !budget.getLastResetDate().isBefore(resetDate)) {
                    skipped++;
                    continue;
                }

                idsToReset.add(budget.getId());
            } catch (Exception e) {
                log.warn("Error checking budget {}: {}", budget.getId(), e.getMessage());
                failed++;
            }
        }

        // Bulk update
        if (!idsToReset.isEmpty()) {
            try {
                int updated = budgetRepository.bulkResetBudgets(idsToReset, resetDate);
                reset = updated;
            } catch (Exception e) {
                log.error("Bulk reset failed for {} budgets: {}", idsToReset.size(), e.getMessage());
                failed += idsToReset.size();
            }
        }

        return new BatchResult(reset, failed, skipped);
    }

    @Override
    public int resetAllOverdueBudgets() {
        log.info("Starting catch-up reset for all overdue budgets");
        
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        List<String> timezones = budgetRepository.findDistinctTimezonesNeedingReset(today);
        
        int totalReset = 0;
        for (String timezone : timezones) {
            try {
                LocalDate tzToday = LocalDate.now(ZoneId.of(timezone != null ? timezone : "UTC"));
                totalReset += resetBudgetsForTimezone(timezone != null ? timezone : "UTC", tzToday);
            } catch (Exception e) {
                log.error("Failed catch-up reset for timezone {}: {}", timezone, e.getMessage());
            }
        }
        
        return totalReset;
    }

    @Override
    public boolean isTimezoneAtMidnight(String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            return now.getHour() == 0;
        } catch (Exception e) {
            log.warn("Invalid timezone {}, defaulting to false", timezone);
            return false;
        }
    }

    @Override
    public List<String> getTimezonesAtMidnight() {
        // Get all standard timezone IDs
        Set<String> allZones = ZoneId.getAvailableZoneIds();
        
        return allZones.stream()
                .filter(this::isTimezoneAtMidnight)
                .collect(Collectors.toList());
    }

    private String getInstanceId() {
        try {
            return applicationName + "-" + InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return applicationName + "-unknown";
        }
    }

    private record BatchResult(int reset, int failed, int skipped) {}
}
