package com.example.chat.model.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String senderId,
        String senderName,
        String content,
        String messageType,
        Instant createdAt
) {
}
