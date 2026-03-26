package com.example.chat.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(300);

    private final RedisTemplate<String, String> redisTemplate;

    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setOnline(String userId) {
        String key = buildKey(userId);
        redisTemplate.opsForValue().set(key, "true", ONLINE_TTL);
    }

    public void setOffline(String userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
    }

    public boolean isOnline(String userId) {
        String key = buildKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public List<String> getOnlineUsers(List<String> userIds) {
        return userIds.stream()
                .filter(this::isOnline)
                .toList();
    }

    private String buildKey(String userId) {
        return "user:%s:online".formatted(userId);
    }
}
