package com.example.chat.config;

import com.example.chat.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        var headers = StompHeaderAccessor.wrap(event.getMessage());
        if (headers.getUser() != null) {
            presenceService.setOnline(headers.getUser().getName());
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        var headers = StompHeaderAccessor.wrap(event.getMessage());
        if (headers.getUser() != null) {
            presenceService.setOffline(headers.getUser().getName());
        }
    }
}
