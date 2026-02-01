package com.my.challenger.scheduler;

import com.my.challenger.service.WagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WagerExpirationScheduler {

    private final WagerService wagerService;

    /**
     * Check for expired wagers every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void checkExpiredWagers() {
        log.debug("Starting scheduled check for expired wagers");
        int expiredCount = wagerService.expireStaleWagers();
        if (expiredCount > 0) {
            log.info("Successfully processed {} expired wagers", expiredCount);
        }
    }
}
