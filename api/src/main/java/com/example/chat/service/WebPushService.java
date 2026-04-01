package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.PushSubscription;
import com.example.chat.repository.PushSubscriptionRepository;
import com.example.chat.repository.RoomMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushService pushService;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ObjectMapper objectMapper;

    public WebPushService(ObjectProvider<PushService> pushServiceProvider,
                          PushSubscriptionRepository pushSubscriptionRepository,
                          RoomMemberRepository roomMemberRepository,
                          ObjectMapper objectMapper) {
        this.pushService = pushServiceProvider.getIfAvailable();
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.objectMapper = objectMapper;
    }

    public void sendPushToMembers(ChatNotificationEvent event) {
        if (pushService == null) {
            log.debug("Web Push is not configured, skipping push for message {}", event.messageId());
            return;
        }

        var members = roomMemberRepository.findByRoomId(event.roomId());
        for (var member : members) {
            if (member.getUserId().equals(event.senderId())) continue;

            var subscriptions = pushSubscriptionRepository.findByUserId(member.getUserId());
            for (var sub : subscriptions) {
                sendPush(sub, event);
            }
        }
    }

    private void sendPush(PushSubscription sub, ChatNotificationEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(Map.of(
                    "roomId", event.roomId().toString(),
                    "senderName", event.senderName(),
                    "contentPreview", truncate(event.content(), 100),
                    "messageType", event.messageType()
            ));

            var notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload
            );

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 410 || statusCode == 404) {
                log.info("Push subscription expired, removing: {}", sub.getEndpoint());
                pushSubscriptionRepository.delete(sub);
            } else if (statusCode >= 400) {
                log.warn("Push failed with status {} for endpoint {}", statusCode, sub.getEndpoint());
            }
        } catch (Exception e) {
            log.warn("Push failed for endpoint {}: {}", sub.getEndpoint(), e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
