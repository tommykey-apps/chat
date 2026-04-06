package com.example.chat.service;

import com.example.chat.model.entity.ChatMessage;

public interface NotificationSender {
    void sendNotification(ChatMessage message);
}
