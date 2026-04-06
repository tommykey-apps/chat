package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationSender.class);

    private final ApplicationEventPublisher eventPublisher;

    public LocalNotificationSender(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendNotification(ChatMessage message) {
        var event = new ChatNotificationEvent(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );

        eventPublisher.publishEvent(event);
        log.debug("Published local notification event for message {}", message.getId());
    }
}
