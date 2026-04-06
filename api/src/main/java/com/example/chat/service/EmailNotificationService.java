package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.repository.RoomMemberRepository;
import com.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final int UNREAD_THRESHOLD = 5;
    private static final Duration EMAIL_COOLDOWN = Duration.ofHours(1);

    private final MailSender mailSender;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final String fromAddress;

    public EmailNotificationService(
            ObjectProvider<MailSender> mailSenderProvider,
            RoomMemberRepository roomMemberRepository,
            UserRepository userRepository,
            RedisTemplate<String, String> redisTemplate,
            @Value("${app.ses.from-address:}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.fromAddress = fromAddress;
    }

    public void sendEmailIfNeeded(ChatNotificationEvent event) {
        if (mailSender == null || fromAddress.isBlank()) {
            log.debug("SES is not configured, skipping email notification");
            return;
        }

        var members = roomMemberRepository.findByRoomId(event.roomId());
        for (var member : members) {
            if (member.getUserId().equals(event.senderId())) continue;

            var unreadCount = getUnreadCount(member.getUserId(), event.roomId().toString());
            if (unreadCount < UNREAD_THRESHOLD) continue;

            var cooldownKey = "email-cooldown:" + member.getUserId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) continue;

            userRepository.findById(member.getUserId()).ifPresent(user -> {
                sendUnreadNotificationEmail(user.getEmail(), user.getDisplayName(),
                        event.senderName(), unreadCount);
                redisTemplate.opsForValue().set(cooldownKey, "1", EMAIL_COOLDOWN);
            });
        }
    }

    private long getUnreadCount(String userId, String roomId) {
        var count = redisTemplate.opsForHash().get("unread:" + userId, roomId);
        if (count == null) return 0;
        try {
            return Long.parseLong(count.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void sendUnreadNotificationEmail(String to, String displayName,
                                              String senderName, long unreadCount) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("chatto: " + unreadCount + "件の未読メッセージがあります");
            message.setText(
                    displayName + " さん\n\n" +
                    senderName + " さんからのメッセージを含む " + unreadCount + " 件の未読メッセージがあります。\n\n" +
                    "chattoを開いて確認しましょう:\nhttps://chat.tommykeyapp.com/rooms\n"
            );
            mailSender.send(message);
            log.info("Sent unread notification email to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
