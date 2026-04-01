package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.dto.NotificationPayload;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Captor
    private ArgumentCaptor<NotificationPayload> payloadCaptor;

    private NotificationService notificationService;

    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String SENDER_ID = "sender-001";
    private static final String SENDER_NAME = "Alice";
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:30:00Z");

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                messagingTemplate, roomMemberRepository, redisTemplate);
    }

    @Test
    void sendsStompNotificationToAllMembersExceptSender() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"),
                createMember("user-003", "Charlie"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var event = createEvent("Hello everyone");
        notificationService.notifyRoomMembers(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-002"), eq("/queue/notifications"), any());
        verify(messagingTemplate).convertAndSendToUser(
                eq("user-003"), eq("/queue/notifications"), any());
        verify(messagingTemplate, times(2))
                .convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void doesNotSendNotificationToSender() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var event = createEvent("Hello");
        notificationService.notifyRoomMembers(event);

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(SENDER_ID), anyString(), any());
    }

    @Test
    void incrementsRedisUnreadCountForEachRecipient() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"),
                createMember("user-003", "Charlie"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var event = createEvent("Hello");
        notificationService.notifyRoomMembers(event);

        verify(hashOperations).increment(
                "unread:user-002", ROOM_ID.toString(), 1);
        verify(hashOperations).increment(
                "unread:user-003", ROOM_ID.toString(), 1);
        verify(hashOperations, never()).increment(
                eq("unread:" + SENDER_ID), anyString(), anyLong());
    }

    @Test
    void truncatesContentLongerThan100Chars() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var longContent = "a".repeat(150);
        var event = createEvent(longContent);
        notificationService.notifyRoomMembers(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-002"), eq("/queue/notifications"), payloadCaptor.capture());

        var payload = payloadCaptor.getValue();
        assertThat(payload.contentPreview()).hasSize(103); // 100 chars + "..."
    }

    @Test
    void handlesNullContentGracefully() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var event = new ChatNotificationEvent(
                MESSAGE_ID, ROOM_ID, SENDER_ID, SENDER_NAME,
                null, "TEXT", CREATED_AT);
        notificationService.notifyRoomMembers(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-002"), eq("/queue/notifications"), payloadCaptor.capture());

        var payload = payloadCaptor.getValue();
        assertThat(payload.contentPreview()).isEmpty();
    }

    @Test
    void worksWithEmptyMemberList() {
        when(roomMemberRepository.findByRoomId(ROOM_ID))
                .thenReturn(Collections.emptyList());

        var event = createEvent("Hello");

        assertThatNoException().isThrownBy(
                () -> notificationService.notifyRoomMembers(event));

        verifyNoInteractions(messagingTemplate);
        verify(redisTemplate, never()).opsForHash();
    }

    private ChatNotificationEvent createEvent(String content) {
        return new ChatNotificationEvent(
                MESSAGE_ID, ROOM_ID, SENDER_ID, SENDER_NAME,
                content, "TEXT", CREATED_AT);
    }

    private RoomMember createMember(String userId, String userName) {
        var member = new RoomMember();
        member.setRoomId(ROOM_ID);
        member.setUserId(userId);
        member.setUserName(userName);
        member.setRole("MEMBER");
        member.setJoinedAt(Instant.now());
        return member;
    }
}
