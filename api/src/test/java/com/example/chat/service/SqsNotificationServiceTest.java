package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.ChatMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsNotificationServiceTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void sendNotification_whenSqsConfigured_sendsToSqs() {
        SqsNotificationService service = buildService(sqsTemplate, "chat-notifications");
        ChatMessage message = buildMessage();

        service.sendNotification(message);

        verify(sqsTemplate).send(any(Consumer.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sendNotification_whenSqsTemplateIsNull_publishesLocalEvent() {
        SqsNotificationService service = buildService(null, "chat-notifications");
        ChatMessage message = buildMessage();

        service.sendNotification(message);

        verify(eventPublisher).publishEvent(any(ChatNotificationEvent.class));
    }

    @Test
    void sendNotification_whenQueueNameIsBlank_publishesLocalEvent() {
        SqsNotificationService service = buildService(sqsTemplate, "");
        ChatMessage message = buildMessage();

        service.sendNotification(message);

        verify(eventPublisher).publishEvent(any(ChatNotificationEvent.class));
        verify(sqsTemplate, never()).send(any(Consumer.class));
    }

    @Test
    void sendNotification_eventHasCorrectFieldsMappedFromChatMessage() {
        SqsNotificationService service = buildService(null, "");
        ChatMessage message = buildMessage();

        service.sendNotification(message);

        ArgumentCaptor<ChatNotificationEvent> captor = ArgumentCaptor.forClass(ChatNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ChatNotificationEvent event = captor.getValue();
        assertThat(event.messageId()).isEqualTo(message.getId());
        assertThat(event.roomId()).isEqualTo(message.getRoomId());
        assertThat(event.senderId()).isEqualTo(message.getSenderId());
        assertThat(event.senderName()).isEqualTo(message.getSenderName());
        assertThat(event.content()).isEqualTo(message.getContent());
        assertThat(event.messageType()).isEqualTo(message.getMessageType());
        assertThat(event.createdAt()).isEqualTo(message.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private SqsNotificationService buildService(SqsTemplate template, String queueName) {
        ObjectProvider<SqsTemplate> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(template);
        return new SqsNotificationService(provider, queueName, eventPublisher);
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
