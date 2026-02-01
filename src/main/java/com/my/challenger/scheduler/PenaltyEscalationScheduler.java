package com.my.challenger.scheduler;

import com.my.challenger.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PenaltyEscalationScheduler {

    private final PenaltyService penaltyService;

    /**
     * Check for overdue penalties every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void escalateOverduePenalties() {
        log.debug("Starting scheduled check for overdue penalties");
        int count = penaltyService.escalateOverduePenalties();
        if (count > 0) {
            log.info("Escalated {} overdue penalties", count);
        }
    }
}
