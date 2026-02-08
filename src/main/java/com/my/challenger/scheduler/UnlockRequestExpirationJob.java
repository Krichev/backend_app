package com.my.challenger.scheduler;

import com.my.challenger.service.UnlockRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnlockRequestExpirationJob {

    private final UnlockRequestService unlockRequestService;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @SchedulerLock(name = "UnlockRequestExpirationJob_expire", lockAtLeastFor = "PT50M", lockAtMostFor = "PT55M")
    public void expirePendingRequests() {
        log.info("Starting scheduled job: expirePendingRequests");
        try {
            unlockRequestService.expirePendingRequests();
            log.info("Finished scheduled job: expirePendingRequests");
        } catch (Exception e) {
            log.error("Error in expirePendingRequests job", e);
        }
    }
}
