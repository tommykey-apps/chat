package com.example.chat.service;

import com.example.chat.model.entity.ChatMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsNotificationServiceTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Test
    void sendNotification_sendsToSqs() {
        var service = new SqsNotificationService(sqsTemplate, "chat-messages");
        ChatMessage message = buildMessage();

        service.sendNotification(message);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    private ChatMessage buildMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID());
        msg.setRoomId(UUID.randomUUID());
        msg.setSenderId("user-42");
        msg.setSenderName("Bob");
        msg.setContent("Test message");
        msg.setMessageType("TEXT");
        msg.setCreatedAt(Instant.parse("2026-01-15T10:30:00Z"));
        return msg;
    }
}
