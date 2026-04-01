package com.example.chat.model.dto;

public record PushSubscriptionRequest(
        String endpoint,
        String p256dh,
        String auth
) {
}
