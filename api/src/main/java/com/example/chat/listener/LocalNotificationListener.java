package com.example.chat.listener;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.service.NotificationService;
import com.example.chat.service.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationListener.class);

    private final NotificationService notificationService;
    private final WebPushService webPushService;

    public LocalNotificationListener(NotificationService notificationService, WebPushService webPushService) {
        this.notificationService = notificationService;
        this.webPushService = webPushService;
    }

    @EventListener
    public void onChatNotification(ChatNotificationEvent event) {
        log.info("Local notification event: messageId={}, roomId={}", event.messageId(), event.roomId());
        notificationService.notifyRoomMembers(event);
        webPushService.sendPushToMembers(event);
    }
}
