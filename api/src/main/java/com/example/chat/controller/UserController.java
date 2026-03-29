package com.example.chat.controller;

import com.example.chat.model.entity.User;
import com.example.chat.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public User me(Principal principal) {
        return userService.getUser(principal.getName());
    }

    @GetMapping("/search")
    public List<User> search(@RequestParam String q) {
        return userService.searchUsers(q);
    }
}
