package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.dto.NotificationPayload;
import com.example.chat.repository.RoomMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomMemberRepository roomMemberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate,
                               RoomMemberRepository roomMemberRepository,
                               RedisTemplate<String, String> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.roomMemberRepository = roomMemberRepository;
        this.redisTemplate = redisTemplate;
    }

    public void notifyRoomMembers(ChatNotificationEvent event) {
        var members = roomMemberRepository.findByRoomId(event.roomId());
        var payload = new NotificationPayload(
                event.roomId(),
                event.messageId(),
                event.senderName(),
                truncate(event.content(), 100),
                event.messageType(),
                event.createdAt()
        );

        for (var member : members) {
            if (member.getUserId().equals(event.senderId())) continue;

            messagingTemplate.convertAndSendToUser(
                    member.getUserId(), "/queue/notifications", payload
            );
            redisTemplate.opsForHash().increment(
                    "unread:" + member.getUserId(), event.roomId().toString(), 1
            );
        }

        log.info("Notified {} members for message {} in room {}",
                members.size() - 1, event.messageId(), event.roomId());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
