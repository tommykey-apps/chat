package com.example.chat.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final RedisTemplate<String, String> redisTemplate;

    public NotificationController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/unread")
    public Map<String, Long> getUnreadCounts(Principal principal) {
        String key = "unread:" + principal.getName();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        var result = new HashMap<String, Long>();
        entries.forEach((roomId, count) ->
                result.put(roomId.toString(), Long.parseLong(count.toString()))
        );
        return result;
    }

    @DeleteMapping("/unread/{roomId}")
    public ResponseEntity<Void> clearUnread(@PathVariable UUID roomId, Principal principal) {
        String key = "unread:" + principal.getName();
        redisTemplate.opsForHash().delete(key, roomId.toString());
        return ResponseEntity.noContent().build();
    }
}
