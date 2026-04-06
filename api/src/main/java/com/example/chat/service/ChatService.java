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
    private final SearchService searchService;
    private final NotificationSender notificationSender;

    public ChatService(ChatMessageRepository chatMessageRepository, SearchService searchService,
                       NotificationSender notificationSender) {
        this.chatMessageRepository = chatMessageRepository;
        this.searchService = searchService;
        this.notificationSender = notificationSender;
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
        var saved = chatMessageRepository.save(message);
        try {
            searchService.indexMessage(saved);
        } catch (Exception e) {
            // ES障害時もメッセージ送信は成功させる
        }
        try {
            notificationSender.sendNotification(saved);
        } catch (Exception e) {
            // SQS障害時もメッセージ送信は成功させる
        }
        return saved;
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
