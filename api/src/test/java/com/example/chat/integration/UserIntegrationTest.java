package com.example.chat.integration;

import com.example.chat.model.entity.User;
import com.example.chat.repository.FriendshipRepository;
import com.example.chat.repository.PushSubscriptionRepository;
import com.example.chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        pushSubscriptionRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("testuser@example.com");
        testUser.setDisplayName("TestUser");
        userRepository.save(testUser);
    }

    @Test
    void getMe_returnsCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(testUser.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.displayName").value("TestUser"));
    }

    @Test
    void searchUsers_returnsMatchingResults() throws Exception {
        var anotherUser = new User();
        anotherUser.setId("another-user-id");
        anotherUser.setEmail("another@example.com");
        anotherUser.setDisplayName("AnotherUser");
        userRepository.save(anotherUser);

        mockMvc.perform(get("/api/users/search")
                        .param("q", "another")
                        .with(jwt().jwt(j -> j.subject(testUser.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("another-user-id"))
                .andExpect(jsonPath("$[0].displayName").value("AnotherUser"));
    }

    @Test
    void searchUsers_noResults_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("q", "nonexistent")
                        .with(jwt().jwt(j -> j.subject(testUser.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
