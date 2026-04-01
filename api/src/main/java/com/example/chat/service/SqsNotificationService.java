package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.ChatMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SqsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationService.class);

    private final SqsTemplate sqsTemplate;
    private final String queueName;
    private final ApplicationEventPublisher eventPublisher;

    public SqsNotificationService(
            ObjectProvider<SqsTemplate> sqsTemplateProvider,
            @Value("${app.sqs.chat-message-queue:}") String queueName,
            ApplicationEventPublisher eventPublisher) {
        this.sqsTemplate = sqsTemplateProvider.getIfAvailable();
        this.queueName = queueName;
        this.eventPublisher = eventPublisher;
    }

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

        if (sqsTemplate == null || queueName.isBlank()) {
            log.debug("SQS is not configured, publishing local event for message {}", message.getId());
            eventPublisher.publishEvent(event);
            return;
        }

        sqsTemplate.send(to -> to.queue(queueName).payload(event));
        log.info("Sent notification to SQS for message {} in room {}", message.getId(), message.getRoomId());
    }
}
