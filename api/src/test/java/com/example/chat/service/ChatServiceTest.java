package com.example.chat.service;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SearchService searchService;

    @Mock
    private SqsNotificationService sqsNotificationService;

    @InjectMocks
    private ChatService chatService;

    // --- saveMessage tests ---

    @Test
    void saveMessage_savesWithCorrectFieldsAndReturnsSavedEntity() {
        UUID roomId = UUID.randomUUID();
        String senderId = "user-1";
        String senderName = "Alice";
        String content = "Hello!";
        String messageType = "TEXT";

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            msg.setCreatedAt(Instant.now());
            return msg;
        });

        ChatMessage result = chatService.saveMessage(roomId, senderId, senderName, content, messageType);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getRoomId()).isEqualTo(roomId);
        assertThat(result.getSenderId()).isEqualTo(senderId);
        assertThat(result.getSenderName()).isEqualTo(senderName);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getMessageType()).isEqualTo(messageType);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getSenderName()).isEqualTo(senderName);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getMessageType()).isEqualTo(messageType);
    }

    @Test
    void saveMessage_callsSearchServiceIndexMessage() {
        UUID roomId = UUID.randomUUID();
        ChatMessage savedMsg = buildSavedMessage(roomId);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        chatService.saveMessage(roomId, "user-1", "Alice", "Hello!", "TEXT");

        verify(searchService).indexMessage(savedMsg);
    }

    @Test
    void saveMessage_callsSqsNotificationServiceSendNotification() {
        UUID roomId = UUID.randomUUID();
        ChatMessage savedMsg = buildSavedMessage(roomId);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        chatService.saveMessage(roomId, "user-1", "Alice", "Hello!", "TEXT");

        verify(sqsNotificationService).sendNotification(savedMsg);
    }

    @Test
    void saveMessage_whenSearchServiceThrows_messageIsStillSavedAndReturned() {
        UUID roomId = UUID.randomUUID();
        ChatMessage savedMsg = buildSavedMessage(roomId);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);
        doThrow(new RuntimeException("ES is down")).when(searchService).indexMessage(any());

        ChatMessage result = chatService.saveMessage(roomId, "user-1", "Alice", "Hello!", "TEXT");

        assertThat(result).isSameAs(savedMsg);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void saveMessage_whenSqsNotificationServiceThrows_messageIsStillSavedAndReturned() {
        UUID roomId = UUID.randomUUID();
        ChatMessage savedMsg = buildSavedMessage(roomId);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);
        doThrow(new RuntimeException("SQS is down")).when(sqsNotificationService).sendNotification(any());

        ChatMessage result = chatService.saveMessage(roomId, "user-1", "Alice", "Hello!", "TEXT");

        assertThat(result).isSameAs(savedMsg);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    // --- getMessages tests ---

    @Test
    void getMessages_returnsMappedMessageResponsePage() {
        UUID roomId = UUID.randomUUID();
        ChatMessage msg = buildSavedMessage(roomId);
        Page<ChatMessage> page = new PageImpl<>(List.of(msg));
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomId), any(PageRequest.class)))
                .thenReturn(page);

        Page<MessageResponse> result = chatService.getMessages(roomId, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        MessageResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(msg.getId());
        assertThat(response.senderId()).isEqualTo(msg.getSenderId());
        assertThat(response.senderName()).isEqualTo(msg.getSenderName());
        assertThat(response.content()).isEqualTo(msg.getContent());
        assertThat(response.messageType()).isEqualTo(msg.getMessageType());
        assertThat(response.createdAt()).isEqualTo(msg.getCreatedAt());
    }

    @Test
    void getMessages_passesCorrectPageRequestToRepository() {
        UUID roomId = UUID.randomUUID();
        int pageNum = 2;
        int size = 10;
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Page.empty());

        chatService.getMessages(roomId, pageNum, size);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(chatMessageRepository).findByRoomIdOrderByCreatedAtDesc(eq(roomId), captor.capture());
        PageRequest captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(pageNum);
        assertThat(captured.getPageSize()).isEqualTo(size);
    }

    // --- helpers ---

    private ChatMessage buildSavedMessage(UUID roomId) {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID());
        msg.setRoomId(roomId);
        msg.setSenderId("user-1");
        msg.setSenderName("Alice");
        msg.setContent("Hello!");
        msg.setMessageType("TEXT");
        msg.setCreatedAt(Instant.now());
        return msg;
    }
}
