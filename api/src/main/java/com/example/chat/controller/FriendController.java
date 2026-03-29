package com.example.chat.controller;

import com.example.chat.model.entity.Friendship;
import com.example.chat.model.entity.User;
import com.example.chat.service.FriendService;
import com.example.chat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    public FriendController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    @GetMapping
    public List<User> listFriends(Principal principal) {
        return friendService.getFriends(principal.getName());
    }

    @GetMapping("/requests")
    public List<Map<String, Object>> listRequests(Principal principal) {
        return friendService.getPendingRequests(principal.getName()).stream()
                .map(f -> {
                    var sender = userService.getUser(f.getUserId());
                    return Map.<String, Object>of(
                            "userId", sender.getId(),
                            "email", sender.getEmail(),
                            "displayName", sender.getDisplayName(),
                            "createdAt", f.getCreatedAt().toString()
                    );
                })
                .toList();
    }

    @PostMapping("/{userId}/request")
    @ResponseStatus(HttpStatus.CREATED)
    public Friendship sendRequest(Principal principal, @PathVariable String userId) {
        return friendService.sendRequest(principal.getName(), userId);
    }

    @PostMapping("/{userId}/accept")
    public Friendship acceptRequest(Principal principal, @PathVariable String userId) {
        return friendService.acceptRequest(principal.getName(), userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(Principal principal, @PathVariable String userId) {
        friendService.removeFriend(principal.getName(), userId);
    }
}
