package com.example.chat.service;

import com.example.chat.model.entity.User;
import com.example.chat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User ensureUser(String id, String email) {
        return userRepository.findById(id).orElseGet(() -> {
            var user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setDisplayName(email.split("@")[0]);
            return userRepository.save(user);
        });
    }

    public User getUser(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User updateDisplayName(String id, String displayName) {
        var user = getUser(id);
        user.setDisplayName(displayName);
        return userRepository.save(user);
    }

    public List<User> searchUsers(String query) {
        return userRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query);
    }
}
