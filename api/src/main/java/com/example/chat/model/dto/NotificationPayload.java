package com.example.chat.model.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationPayload(
        UUID roomId,
        UUID messageId,
        String senderName,
        String contentPreview,
        String messageType,
        Instant createdAt
) {
}
