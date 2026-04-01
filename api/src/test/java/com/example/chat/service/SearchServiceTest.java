package com.example.chat.service;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.model.entity.ChatMessageDocument;
import com.example.chat.repository.ChatMessageSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ChatMessageSearchRepository chatMessageSearchRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void indexMessage_convertsChatMessageToDocumentAndSaves() {
        ChatMessage message = new ChatMessage();
        UUID messageId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Instant now = Instant.now();

        message.setId(messageId);
        message.setRoomId(roomId);
        message.setSenderId("user-1");
        message.setSenderName("Alice");
        message.setContent("Hello, world!");
        message.setMessageType("TEXT");
        message.setCreatedAt(now);

        searchService.indexMessage(message);

        ArgumentCaptor<ChatMessageDocument> captor = ArgumentCaptor.forClass(ChatMessageDocument.class);
        verify(chatMessageSearchRepository).save(captor.capture());

        ChatMessageDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(messageId.toString());
        assertThat(saved.getRoomId()).isEqualTo(roomId.toString());
        assertThat(saved.getSenderId()).isEqualTo("user-1");
        assertThat(saved.getSenderName()).isEqualTo("Alice");
        assertThat(saved.getContent()).isEqualTo("Hello, world!");
        assertThat(saved.getMessageType()).isEqualTo("TEXT");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void searchMessages_returnsPageOfMessageResponses() {
        UUID roomId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        String query = "hello";
        int page = 0;
        int size = 10;

        ChatMessageDocument doc = new ChatMessageDocument();
        // Use reflection or create via ChatMessage constructor
        ChatMessage msg = new ChatMessage();
        msg.setId(docId);
        msg.setRoomId(roomId);
        msg.setSenderId("user-2");
        msg.setSenderName("Bob");
        msg.setContent("hello there");
        msg.setMessageType("TEXT");
        msg.setCreatedAt(now);
        ChatMessageDocument document = new ChatMessageDocument(msg);

        Pageable expectedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessageDocument> docPage = new PageImpl<>(List.of(document), expectedPageable, 1);

        when(chatMessageSearchRepository.findByRoomIdAndContentContaining(
                eq(roomId.toString()), eq(query), eq(expectedPageable)))
                .thenReturn(docPage);

        Page<MessageResponse> result = searchService.searchMessages(roomId, query, page, size);

        assertThat(result.getTotalElements()).isEqualTo(1);
        MessageResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(docId);
        assertThat(response.senderId()).isEqualTo("user-2");
        assertThat(response.senderName()).isEqualTo("Bob");
        assertThat(response.content()).isEqualTo("hello there");
        assertThat(response.messageType()).isEqualTo("TEXT");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void searchMessages_returnsEmptyPageWhenNoResults() {
        UUID roomId = UUID.randomUUID();
        String query = "nonexistent";
        int page = 0;
        int size = 10;

        Pageable expectedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessageDocument> emptyPage = new PageImpl<>(List.of(), expectedPageable, 0);

        when(chatMessageSearchRepository.findByRoomIdAndContentContaining(
                eq(roomId.toString()), eq(query), eq(expectedPageable)))
                .thenReturn(emptyPage);

        Page<MessageResponse> result = searchService.searchMessages(roomId, query, page, size);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
