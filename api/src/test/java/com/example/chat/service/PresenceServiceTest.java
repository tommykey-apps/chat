package com.example.chat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    void setOnline_setsRedisKeyWithTtl() {
        String userId = "user-1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        presenceService.setOnline(userId);

        verify(valueOperations).set("user:user-1:online", "true", Duration.ofSeconds(300));
    }

    @Test
    void setOffline_deletesRedisKey() {
        String userId = "user-1";

        presenceService.setOffline(userId);

        verify(redisTemplate).delete("user:user-1:online");
    }

    @Test
    void isOnline_returnsTrueWhenKeyExists() {
        String userId = "user-1";
        when(redisTemplate.hasKey("user:user-1:online")).thenReturn(true);

        boolean result = presenceService.isOnline(userId);

        assertThat(result).isTrue();
    }

    @Test
    void isOnline_returnsFalseWhenKeyDoesNotExist() {
        String userId = "user-1";
        when(redisTemplate.hasKey("user:user-1:online")).thenReturn(false);

        boolean result = presenceService.isOnline(userId);

        assertThat(result).isFalse();
    }

    @Test
    void isOnline_returnsFalseWhenHasKeyReturnsNull() {
        String userId = "user-1";
        when(redisTemplate.hasKey("user:user-1:online")).thenReturn(null);

        boolean result = presenceService.isOnline(userId);

        assertThat(result).isFalse();
    }

    @Test
    void getOnlineUsers_filtersToOnlyOnlineUsers() {
        when(redisTemplate.hasKey("user:user-1:online")).thenReturn(true);
        when(redisTemplate.hasKey("user:user-2:online")).thenReturn(false);
        when(redisTemplate.hasKey("user:user-3:online")).thenReturn(true);

        List<String> result = presenceService.getOnlineUsers(List.of("user-1", "user-2", "user-3"));

        assertThat(result).containsExactly("user-1", "user-3");
    }

    @Test
    void getOnlineUsers_returnsEmptyListWhenNoOneIsOnline() {
        when(redisTemplate.hasKey("user:user-1:online")).thenReturn(false);
        when(redisTemplate.hasKey("user:user-2:online")).thenReturn(false);

        List<String> result = presenceService.getOnlineUsers(List.of("user-1", "user-2"));

        assertThat(result).isEmpty();
    }

    @Test
    void getOnlineUsers_returnsEmptyListForEmptyInput() {
        List<String> result = presenceService.getOnlineUsers(List.of());

        assertThat(result).isEmpty();
    }
}
