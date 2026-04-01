package com.example.chat.integration;

import com.example.chat.model.entity.User;
import com.example.chat.repository.FriendshipRepository;
import com.example.chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FriendIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setId("user-a");
        userA.setEmail("alice@example.com");
        userA.setDisplayName("Alice");
        userRepository.save(userA);

        userB = new User();
        userB.setId("user-b");
        userB.setEmail("bob@example.com");
        userB.setDisplayName("Bob");
        userRepository.save(userB);
    }

    @Test
    void sendFriendRequest_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/friends/{userId}/request", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userA.getId()))
                .andExpect(jsonPath("$.friendId").value(userB.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void acceptFriendRequest_updatesFriendshipStatus() throws Exception {
        // user-a sends request to user-b
        mockMvc.perform(post("/api/friends/{userId}/request", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isCreated());

        // user-b accepts
        mockMvc.perform(post("/api/friends/{userId}/accept", userA.getId())
                        .with(jwt().jwt(j -> j.subject(userB.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void listFriends_returnsOnlyAcceptedFriends() throws Exception {
        // Create and accept a friendship
        mockMvc.perform(post("/api/friends/{userId}/request", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/friends/{userId}/accept", userA.getId())
                        .with(jwt().jwt(j -> j.subject(userB.getId()))))
                .andExpect(status().isOk());

        // Create a third user with a pending request
        var userC = new User();
        userC.setId("user-c");
        userC.setEmail("charlie@example.com");
        userC.setDisplayName("Charlie");
        userRepository.save(userC);

        mockMvc.perform(post("/api/friends/{userId}/request", userA.getId())
                        .with(jwt().jwt(j -> j.subject(userC.getId()))))
                .andExpect(status().isCreated());

        // List friends for user-a: should only contain user-b (accepted), not user-c (pending)
        mockMvc.perform(get("/api/friends")
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userB.getId()));
    }

    @Test
    void listPendingRequests_returnsPendingRequests() throws Exception {
        // user-a sends request to user-b
        mockMvc.perform(post("/api/friends/{userId}/request", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isCreated());

        // user-b checks pending requests
        mockMvc.perform(get("/api/friends/requests")
                        .with(jwt().jwt(j -> j.subject(userB.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(userA.getId()))
                .andExpect(jsonPath("$[0].displayName").value("Alice"));
    }

    @Test
    void removeFriend_deletesFriendship() throws Exception {
        // Create and accept friendship
        mockMvc.perform(post("/api/friends/{userId}/request", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/friends/{userId}/accept", userA.getId())
                        .with(jwt().jwt(j -> j.subject(userB.getId()))))
                .andExpect(status().isOk());

        // Remove friend
        mockMvc.perform(delete("/api/friends/{userId}", userB.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isNoContent());

        // Verify friend list is now empty
        mockMvc.perform(get("/api/friends")
                        .with(jwt().jwt(j -> j.subject(userA.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sendFriendRequest_toSelf_throwsException() {
        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/friends/{userId}/request", userA.getId())
                        .with(jwt().jwt(j -> j.subject(userA.getId())))));
    }
}
