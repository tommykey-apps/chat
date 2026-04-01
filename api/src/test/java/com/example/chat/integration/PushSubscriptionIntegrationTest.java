package com.example.chat.integration;

import com.example.chat.model.entity.User;
import com.example.chat.repository.PushSubscriptionRepository;
import com.example.chat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PushSubscriptionIntegrationTest extends BaseIntegrationTest {

    private static final String USER_ID = "test-user-push";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        pushSubscriptionRepository.deleteAll();

        if (userRepository.findById(USER_ID).isEmpty()) {
            User user = new User();
            user.setId(USER_ID);
            user.setEmail("push-test@example.com");
            user.setDisplayName("Push Test User");
            userRepository.save(user);
        }
    }

    @Test
    void getVapidKey_returnsPublicKey() throws Exception {
        mockMvc.perform(get("/api/push/vapid-key")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(""));
    }

    @Test
    void subscribe_storesSubscriptionInDb() throws Exception {
        var request = Map.of(
                "endpoint", "https://push.example.com/send/abc123",
                "p256dh", "test-p256dh-key",
                "auth", "test-auth-key"
        );

        mockMvc.perform(post("/api/push/subscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var subscriptions = pushSubscriptionRepository.findByUserId(USER_ID);
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getEndpoint()).isEqualTo("https://push.example.com/send/abc123");
        assertThat(subscriptions.get(0).getP256dh()).isEqualTo("test-p256dh-key");
        assertThat(subscriptions.get(0).getAuth()).isEqualTo("test-auth-key");
    }

    @Test
    void subscribe_sameEndpointTwice_noDuplicate() throws Exception {
        var request = Map.of(
                "endpoint", "https://push.example.com/send/dup",
                "p256dh", "test-p256dh-key",
                "auth", "test-auth-key"
        );
        String body = objectMapper.writeValueAsString(request);

        // Subscribe first time
        mockMvc.perform(post("/api/push/subscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Subscribe second time with same endpoint
        mockMvc.perform(post("/api/push/subscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var subscriptions = pushSubscriptionRepository.findByUserId(USER_ID);
        assertThat(subscriptions).hasSize(1);
    }

    @Test
    void unsubscribe_deletesSubscriptionFromDb() throws Exception {
        // First subscribe
        var subscribeRequest = Map.of(
                "endpoint", "https://push.example.com/send/to-delete",
                "p256dh", "test-p256dh-key",
                "auth", "test-auth-key"
        );

        mockMvc.perform(post("/api/push/subscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscribeRequest)))
                .andExpect(status().isOk());

        assertThat(pushSubscriptionRepository.findByUserId(USER_ID)).hasSize(1);

        // Then unsubscribe
        var unsubscribeRequest = Map.of("endpoint", "https://push.example.com/send/to-delete");

        mockMvc.perform(delete("/api/push/unsubscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsubscribeRequest)))
                .andExpect(status().isNoContent());

        assertThat(pushSubscriptionRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void unsubscribe_nonExistentEndpoint_returns204() throws Exception {
        var request = Map.of("endpoint", "https://push.example.com/send/nonexistent");

        mockMvc.perform(delete("/api/push/unsubscribe")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
