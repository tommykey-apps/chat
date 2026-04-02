package com.example.chat.controller;

import com.example.chat.model.entity.User;
import com.example.chat.service.PresenceService;
import com.example.chat.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PresenceService presenceService;

    public UserController(UserService userService, PresenceService presenceService) {
        this.userService = userService;
        this.presenceService = presenceService;
    }

    @GetMapping("/me")
    public User me(Principal principal) {
        return userService.getUser(principal.getName());
    }

    @PatchMapping("/me")
    public User updateProfile(@RequestBody Map<String, String> body, Principal principal) {
        String displayName = body.get("displayName");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        return userService.updateDisplayName(principal.getName(), displayName.trim());
    }

    @GetMapping("/search")
    public List<User> search(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    @GetMapping("/online")
    public List<String> getOnlineUsers(@RequestParam List<String> ids) {
        return presenceService.getOnlineUsers(ids);
    }
}
