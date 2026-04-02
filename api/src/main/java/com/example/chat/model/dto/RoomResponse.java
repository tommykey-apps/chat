package com.example.chat.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        String description,
        String createdBy,
        Instant createdAt,
        int memberCount,
        String lastMessage,
        Instant lastMessageAt
) {
}
