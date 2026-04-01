package com.example.chat.integration;

import com.example.chat.model.entity.ChatMessage;
import com.example.chat.model.entity.ChatRoom;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.model.entity.User;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.FriendshipRepository;
import com.example.chat.repository.PushSubscriptionRepository;
import com.example.chat.repository.RoomMemberRepository;
import com.example.chat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    private static final String USER_ID = "test-user-id";
    private static final String OTHER_USER_ID = "other-user-id";

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        roomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        friendshipRepository.deleteAll();
        pushSubscriptionRepository.deleteAll();
        userRepository.deleteAll();

        var testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        userRepository.save(testUser);

        var otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setEmail("other@example.com");
        otherUser.setDisplayName("Other User");
        userRepository.save(otherUser);
    }

    @Test
    void createRoom_returnsCreatedRoomWithMemberCount1() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "test-room", "description": "A test room"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-room"))
                .andExpect(jsonPath("$.description").value("A test room"))
                .andExpect(jsonPath("$.createdBy").value(USER_ID))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void listRooms_returnsOnlyRoomsUserBelongsTo() throws Exception {
        // Create a room the user is a member of
        var userRoom = createRoomInDb("user-room", "desc1", USER_ID);
        addMemberToRoom(userRoom.getId(), USER_ID, "Test User", "OWNER");

        // Create a room the user is NOT a member of
        var otherRoom = createRoomInDb("other-room", "desc2", OTHER_USER_ID);
        addMemberToRoom(otherRoom.getId(), OTHER_USER_ID, "Other User", "OWNER");

        mockMvc.perform(get("/api/rooms")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("user-room"));
    }

    @Test
    void getRoom_returnsRoomDetailsWithMemberCount() throws Exception {
        var room = createRoomInDb("detail-room", "detail desc", USER_ID);
        addMemberToRoom(room.getId(), USER_ID, "Test User", "OWNER");
        addMemberToRoom(room.getId(), OTHER_USER_ID, "Other User", "MEMBER");

        mockMvc.perform(get("/api/rooms/{roomId}", room.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("detail-room"))
                .andExpect(jsonPath("$.description").value("detail desc"))
                .andExpect(jsonPath("$.memberCount").value(2));
    }

    @Test
    void joinRoom_increasesMemberCount() throws Exception {
        var room = createRoomInDb("join-room", "join desc", OTHER_USER_ID);
        addMemberToRoom(room.getId(), OTHER_USER_ID, "Other User", "OWNER");

        // Join the room
        mockMvc.perform(post("/api/rooms/{roomId}/join", room.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNoContent());

        // Verify member count increased
        mockMvc.perform(get("/api/rooms/{roomId}", room.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2));
    }

    @Test
    void leaveRoom_removesMembership() throws Exception {
        var room = createRoomInDb("leave-room", "leave desc", USER_ID);
        addMemberToRoom(room.getId(), USER_ID, "Test User", "OWNER");
        addMemberToRoom(room.getId(), OTHER_USER_ID, "Other User", "MEMBER");

        // Leave the room
        mockMvc.perform(delete("/api/rooms/{roomId}/leave", room.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNoContent());

        // Verify user's rooms no longer include this room
        mockMvc.perform(get("/api/rooms")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMessages_returnsPaginatedMessages() throws Exception {
        var room = createRoomInDb("msg-room", "msg desc", USER_ID);
        addMemberToRoom(room.getId(), USER_ID, "Test User", "OWNER");

        // Insert messages directly
        for (int i = 0; i < 3; i++) {
            var msg = new ChatMessage();
            msg.setRoomId(room.getId());
            msg.setSenderId(USER_ID);
            msg.setSenderName("Test User");
            msg.setContent("Message " + i);
            msg.setMessageType("TEXT");
            chatMessageRepository.save(msg);
        }

        mockMvc.perform(get("/api/rooms/{roomId}/messages", room.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].senderId").value(USER_ID))
                .andExpect(jsonPath("$.content[0].messageType").value("TEXT"));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());
    }

    private ChatRoom createRoomInDb(String name, String description, String createdBy) {
        var room = new ChatRoom();
        room.setName(name);
        room.setDescription(description);
        room.setCreatedBy(createdBy);
        return chatRoomRepository.save(room);
    }

    private void addMemberToRoom(UUID roomId, String userId, String userName, String role) {
        var member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setUserName(userName);
        member.setRole(role);
        roomMemberRepository.save(member);
    }
}
