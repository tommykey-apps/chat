package com.example.chat.config;

import com.example.chat.service.PresenceService;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class PresenceRefreshScheduler {

    private final SimpUserRegistry simpUserRegistry;
    private final PresenceService presenceService;

    public PresenceRefreshScheduler(SimpUserRegistry simpUserRegistry, PresenceService presenceService) {
        this.simpUserRegistry = simpUserRegistry;
        this.presenceService = presenceService;
    }

    @Scheduled(fixedRate = 120_000)
    public void refreshPresence() {
        simpUserRegistry.getUsers().forEach(user ->
                presenceService.setOnline(user.getName())
        );
    }
}
