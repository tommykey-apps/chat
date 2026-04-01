package com.example.chat.integration;

import com.example.chat.model.entity.User;
import com.example.chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationIntegrationTest extends BaseIntegrationTest {

    private static final String USER_ID = "test-user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up Redis
        String key = "unread:" + USER_ID;
        redisTemplate.delete(key);

        // Ensure user exists in DB
        if (userRepository.findById(USER_ID).isEmpty()) {
            User user = new User();
            user.setId(USER_ID);
            user.setEmail("test@example.com");
            user.setDisplayName("Test User");
            userRepository.save(user);
        }
    }

    @Test
    void getUnreadCounts_whenNoData_returnsEmptyMap() throws Exception {
        mockMvc.perform(get("/api/notifications/unread")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void getUnreadCounts_whenDataExists_returnsCorrectCounts() throws Exception {
        String roomId1 = UUID.randomUUID().toString();
        String roomId2 = UUID.randomUUID().toString();
        String key = "unread:" + USER_ID;

        redisTemplate.opsForHash().put(key, roomId1, "5");
        redisTemplate.opsForHash().put(key, roomId2, "3");

        mockMvc.perform(get("/api/notifications/unread")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + roomId1).value(5))
                .andExpect(jsonPath("$." + roomId2).value(3));
    }

    @Test
    void clearUnread_removesCountForRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        String key = "unread:" + USER_ID;

        redisTemplate.opsForHash().put(key, roomId.toString(), "5");

        mockMvc.perform(delete("/api/notifications/unread/{roomId}", roomId)
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNoContent());

        // Verify the room's count is removed
        mockMvc.perform(get("/api/notifications/unread")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + roomId).doesNotExist());
    }

    @Test
    void clearUnread_nonExistentRoom_returns204() throws Exception {
        UUID nonExistentRoomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/notifications/unread/{roomId}", nonExistentRoomId)
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNoContent());
    }
}
