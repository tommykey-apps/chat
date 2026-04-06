package com.example.chat.model.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchResponse(
        UUID id,
        UUID roomId,
        String senderId,
        String senderName,
        String content,
        String highlightedContent,
        String messageType,
        Instant createdAt
) {}
