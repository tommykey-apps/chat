package com.example.chat.controller;

import com.example.chat.model.dto.MessageRequest;
import com.example.chat.model.dto.MessageResponse;
import com.example.chat.service.ChatService;
import com.example.chat.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{roomId}")
    public void handleMessage(@DestinationVariable UUID roomId,
                              @Payload MessageRequest request,
                              Principal principal) {
        String senderId = principal.getName();
        String senderName = userService.getUser(senderId).getDisplayName();
        String messageType = request.messageType() != null ? request.messageType() : "TEXT";

        var saved = chatService.saveMessage(roomId, senderId, senderName, request.content(), messageType);

        var response = new MessageResponse(
                saved.getId(),
                saved.getSenderId(),
                saved.getSenderName(),
                saved.getContent(),
                saved.getMessageType(),
                saved.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/room." + roomId, response);
    }
}
