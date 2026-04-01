package com.example.chat.service;

import com.example.chat.model.dto.ChatNotificationEvent;
import com.example.chat.model.entity.PushSubscription;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.repository.PushSubscriptionRepository;
import com.example.chat.repository.RoomMemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    private PushService pushService;

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebPushService webPushService;

    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String SENDER_ID = "sender-001";
    private static final String SENDER_NAME = "Alice";
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:30:00Z");

    @BeforeEach
    void setUp() {
        webPushService = createServiceWithPushService(pushService);
    }

    @Test
    void whenPushServiceIsNullDoesNothing() {
        webPushService = createServiceWithPushService(null);

        webPushService.sendPushToMembers(createEvent("Hello"));

        verifyNoInteractions(roomMemberRepository);
        verifyNoInteractions(pushSubscriptionRepository);
    }

    @Test
    void lookupSubscriptionsForAllMembersExceptSender() {
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"),
                createMember("user-003", "Charlie"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);
        when(pushSubscriptionRepository.findByUserId("user-002")).thenReturn(List.of());
        when(pushSubscriptionRepository.findByUserId("user-003")).thenReturn(List.of());

        webPushService.sendPushToMembers(createEvent("Hello"));

        verify(pushSubscriptionRepository).findByUserId("user-002");
        verify(pushSubscriptionRepository).findByUserId("user-003");
        verify(pushSubscriptionRepository, never()).findByUserId(SENDER_ID);
    }

    @Test
    void doesNotSendPushToSenderSubscriptions() {
        var members = List.of(
                createMember(SENDER_ID, "Alice"),
                createMember("user-002", "Bob"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);
        when(pushSubscriptionRepository.findByUserId("user-002")).thenReturn(List.of());

        webPushService.sendPushToMembers(createEvent("Hello"));

        verify(pushSubscriptionRepository, never()).findByUserId(SENDER_ID);
    }

    @Test
    void handlesExceptionGracefullyWhenSubscriptionExists() {
        var members = List.of(createMember("user-002", "Bob"));
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(members);

        var sub = createSubscription("user-002", "https://push.example.com/error");
        when(pushSubscriptionRepository.findByUserId("user-002")).thenReturn(List.of(sub));

        assertThatNoException().isThrownBy(
                () -> webPushService.sendPushToMembers(createEvent("Hello")));
    }

    @Test
    void worksWithEmptyMemberList() {
        when(roomMemberRepository.findByRoomId(ROOM_ID)).thenReturn(List.of());

        assertThatNoException().isThrownBy(
                () -> webPushService.sendPushToMembers(createEvent("Hello")));

        verifyNoInteractions(pushSubscriptionRepository);
    }

    private WebPushService createServiceWithPushService(PushService ps) {
        @SuppressWarnings("unchecked")
        ObjectProvider<PushService> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(ps);
        return new WebPushService(provider, pushSubscriptionRepository,
                roomMemberRepository, objectMapper);
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

    private PushSubscription createSubscription(String userId, String endpoint) {
        var sub = new PushSubscription();
        sub.setId(UUID.randomUUID());
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh("test-p256dh");
        sub.setAuth("test-auth");
        sub.setCreatedAt(Instant.now());
        return sub;
    }
}
