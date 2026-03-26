package com.example.chat.service;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatMessage saveMessage(UUID roomId, String senderId, String senderName,
                                   String content, String messageType) {
        var message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setMessageType(messageType);
        return chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID roomId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
                .map(msg -> new MessageResponse(
                        msg.getId(),
                        msg.getSenderId(),
                        msg.getSenderName(),
                        msg.getContent(),
                        msg.getMessageType(),
                        msg.getCreatedAt()
                ));
    }
}
