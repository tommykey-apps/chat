package com.example.chat.listener;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.service.EmailNotificationService;
import com.example.chat.service.NotificationService;
import com.example.chat.service.WebPushService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqsMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageListener.class);

    private final NotificationService notificationService;
    private final WebPushService webPushService;
    private final EmailNotificationService emailNotificationService;

    public SqsMessageListener(NotificationService notificationService,
                               WebPushService webPushService,
                               EmailNotificationService emailNotificationService) {
        this.notificationService = notificationService;
        this.webPushService = webPushService;
        this.emailNotificationService = emailNotificationService;
    }

    @SqsListener("${app.sqs.chat-message-queue}")
    public void onMessage(ChatNotificationEvent event) {
        log.info("Processing notification: messageId={}, roomId={}, senderId={}",
                event.messageId(), event.roomId(), event.senderId());
        notificationService.notifyRoomMembers(event);
        webPushService.sendPushToMembers(event);
        emailNotificationService.sendEmailIfNeeded(event);
    }
}
