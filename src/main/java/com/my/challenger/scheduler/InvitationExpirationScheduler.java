package com.my.challenger.scheduler;

import com.my.challenger.service.QuestInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationExpirationScheduler {

    private final QuestInvitationService invitationService;

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void expireStaleInvitations() {
        log.info("Starting scheduled job: expireStaleInvitations");
        int count = invitationService.expireStaleInvitations();
        if (count > 0) {
            log.info("Expired {} stale invitations", count);
        }
    }
}
