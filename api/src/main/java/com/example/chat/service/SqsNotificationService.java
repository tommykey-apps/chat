package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.ChatMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class SqsNotificationService implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SqsNotificationService.class);

    private final SqsTemplate sqsTemplate;
    private final String queueName;

    public SqsNotificationService(
            SqsTemplate sqsTemplate,
            @Value("${app.sqs.chat-message-queue}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.queueName = queueName;
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

        sqsTemplate.send(to -> to.queue(queueName).payload(event));
        log.info("Sent notification to SQS for message {} in room {}", message.getId(), message.getRoomId());
    }
}
