package com.example.chat.service;

import com.example.chat.model.dto.RoomResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.model.entity.ChatRoom;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.model.entity.User;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.RoomMemberRepository;
import com.example.chat.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RoomService roomService;

    @Test
    void createRoom_savesRoomAndOwnerMember_returnsRoomResponse() {
        var roomId = UUID.randomUUID();
        var now = Instant.now();

        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            room.setId(roomId);
            room.setCreatedAt(now);
            return room;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.createRoom("general", "General chat", "user1", "Alice", null);

        assertThat(response.id()).isEqualTo(roomId);
        assertThat(response.name()).isEqualTo("general");
        assertThat(response.description()).isEqualTo("General chat");
        assertThat(response.createdBy()).isEqualTo("user1");
        assertThat(response.memberCount()).isEqualTo(1);

        ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
        verify(roomMemberRepository).save(memberCaptor.capture());
        RoomMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getRoomId()).isEqualTo(roomId);
        assertThat(savedMember.getUserId()).isEqualTo("user1");
        assertThat(savedMember.getUserName()).isEqualTo("Alice");
        assertThat(savedMember.getRole()).isEqualTo("OWNER");
    }

    @Test
    void createRoom_withMemberIds_addsMembers() {
        var roomId = UUID.randomUUID();
        var now = Instant.now();

        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            room.setId(roomId);
            room.setCreatedAt(now);
            return room;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var friend = new User();
        friend.setId("user2");
        friend.setDisplayName("Bob");
        when(userRepository.findById("user2")).thenReturn(Optional.of(friend));

        RoomResponse response = roomService.createRoom("Bob", "DM", "user1", "Alice", List.of("user2"));

        assertThat(response.memberCount()).isEqualTo(2);
        verify(roomMemberRepository, times(2)).save(any(RoomMember.class));
    }

    @Test
    void createRoom_withMemberIds_skipsCreatorId() {
        var roomId = UUID.randomUUID();

        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            room.setId(roomId);
            room.setCreatedAt(Instant.now());
            return room;
        });
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.createRoom("Self", "DM", "user1", "Alice", List.of("user1"));

        assertThat(response.memberCount()).isEqualTo(1);
        verify(roomMemberRepository, times(1)).save(any(RoomMember.class));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void listRooms_returnsMemberRoomsWithCounts() {
        var roomId1 = UUID.randomUUID();
        var roomId2 = UUID.randomUUID();
        var now = Instant.now();

        var membership1 = new RoomMember();
        membership1.setRoomId(roomId1);
        membership1.setUserId("user1");
        var membership2 = new RoomMember();
        membership2.setRoomId(roomId2);
        membership2.setUserId("user1");

        when(roomMemberRepository.findByUserId("user1")).thenReturn(List.of(membership1, membership2));

        var room1 = new ChatRoom();
        room1.setId(roomId1);
        room1.setName("room1");
        room1.setCreatedBy("user1");
        room1.setCreatedAt(now);
        var room2 = new ChatRoom();
        room2.setId(roomId2);
        room2.setName("room2");
        room2.setCreatedBy("user2");
        room2.setCreatedAt(now);

        when(chatRoomRepository.findById(roomId1)).thenReturn(Optional.of(room1));
        when(chatRoomRepository.findById(roomId2)).thenReturn(Optional.of(room2));

        var member1 = new RoomMember();
        var member2 = new RoomMember();
        var member3 = new RoomMember();
        when(roomMemberRepository.findByRoomId(roomId1)).thenReturn(List.of(member1, member2));
        when(roomMemberRepository.findByRoomId(roomId2)).thenReturn(List.of(member3));

        List<RoomResponse> rooms = roomService.listRooms("user1");

        assertThat(rooms).hasSize(2);
        assertThat(rooms.get(0).name()).isEqualTo("room1");
        assertThat(rooms.get(0).memberCount()).isEqualTo(2);
        assertThat(rooms.get(1).name()).isEqualTo("room2");
        assertThat(rooms.get(1).memberCount()).isEqualTo(1);
    }

    @Test
    void getRoom_returnsRoomResponseWithMemberCount() {
        var roomId = UUID.randomUUID();
        var now = Instant.now();

        var room = new ChatRoom();
        room.setId(roomId);
        room.setName("test-room");
        room.setDescription("A test room");
        room.setCreatedBy("user1");
        room.setCreatedAt(now);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.findByRoomId(roomId)).thenReturn(List.of(new RoomMember(), new RoomMember(), new RoomMember()));

        RoomResponse response = roomService.getRoom(roomId);

        assertThat(response.id()).isEqualTo(roomId);
        assertThat(response.name()).isEqualTo("test-room");
        assertThat(response.memberCount()).isEqualTo(3);
    }

    @Test
    void getRoom_throwsEntityNotFoundExceptionWhenRoomNotFound() {
        var roomId = UUID.randomUUID();
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(roomId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void joinRoom_savesMemberAndBroadcastsSystemMessage() {
        var roomId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var now = Instant.now();

        when(roomMemberRepository.findByRoomIdAndUserId(roomId, "user2")).thenReturn(Optional.empty());
        when(roomMemberRepository.save(any(RoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var systemMessage = new ChatMessage();
        systemMessage.setId(messageId);
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderName("SYSTEM");
        systemMessage.setContent("Bob joined");
        systemMessage.setMessageType("SYSTEM");
        systemMessage.setCreatedAt(now);

        when(chatService.saveMessage(roomId, "SYSTEM", "SYSTEM", "Bob joined", "SYSTEM"))
                .thenReturn(systemMessage);

        roomService.joinRoom(roomId, "user2", "Bob");

        ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
        verify(roomMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo("user2");
        assertThat(memberCaptor.getValue().getUserName()).isEqualTo("Bob");

        verify(messagingTemplate).convertAndSend(eq("/topic/room." + roomId), any(com.example.chat.model.dto.MessageResponse.class));
    }

    @Test
    void joinRoom_doesNothingWhenAlreadyMember() {
        var roomId = UUID.randomUUID();
        var existingMember = new RoomMember();

        when(roomMemberRepository.findByRoomIdAndUserId(roomId, "user1")).thenReturn(Optional.of(existingMember));

        roomService.joinRoom(roomId, "user1", "Alice");

        verify(roomMemberRepository, never()).save(any());
        verify(chatService, never()).saveMessage(any(), any(), any(), any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void leaveRoom_deletesMembership() {
        var roomId = UUID.randomUUID();

        roomService.leaveRoom(roomId, "user1");

        verify(roomMemberRepository).deleteByRoomIdAndUserId(roomId, "user1");
    }
}
