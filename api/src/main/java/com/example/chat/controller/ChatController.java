package com.example.chat.controller;

import com.example.chat.model.dto.MessageRequest;
import com.example.chat.model.dto.MessageResponse;
import com.example.chat.service.ChatService;
import com.example.chat.service.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public ChatController(ChatService chatService, UserService userService,
                          SimpMessagingTemplate messagingTemplate, RedisTemplate<String, String> redisTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
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

    @MessageMapping("/typing/{roomId}")
    public void handleTyping(@DestinationVariable UUID roomId, Principal principal) {
        String senderId = principal.getName();
        String senderName = userService.getUser(senderId).getDisplayName();
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing",
                Map.of("userId", senderId, "userName", senderName));
    }

    @MessageMapping("/read/{roomId}")
    public void handleRead(@DestinationVariable UUID roomId,
                           @Payload Map<String, String> payload,
                           Principal principal) {
        String userId = principal.getName();
        String messageId = payload.get("messageId");
        redisTemplate.opsForValue().set("read:" + roomId + ":" + userId, messageId);
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".read",
                Map.of("userId", userId, "messageId", messageId));
    }
}
