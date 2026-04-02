package com.example.chat.controller;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.repository.ChatMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(ChatMessageRepository chatMessageRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PutMapping("/{messageId}")
    public MessageResponse editMessage(@PathVariable UUID messageId,
                                       @RequestBody Map<String, String> body,
                                       Principal principal) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!message.getSenderId().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        message.setContent(body.get("content"));
        var saved = chatMessageRepository.save(message);

        var response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/room." + saved.getRoomId() + ".update", response);
        return response;
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId, Principal principal) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!message.getSenderId().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        UUID roomId = message.getRoomId();
        chatMessageRepository.delete(message);
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".delete",
                Map.of("messageId", messageId.toString()));
    }

    private MessageResponse toResponse(ChatMessage msg) {
        return new MessageResponse(msg.getId(), msg.getSenderId(), msg.getSenderName(),
                msg.getContent(), msg.getMessageType(), msg.getCreatedAt());
    }
}
