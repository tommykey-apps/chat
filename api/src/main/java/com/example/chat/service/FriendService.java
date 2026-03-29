package com.example.chat.service;

import com.example.chat.model.entity.Friendship;
import com.example.chat.model.entity.User;
import com.example.chat.repository.FriendshipRepository;
import com.example.chat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Friendship sendRequest(String userId, String friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        friendshipRepository.findBetween(userId, friendId).ifPresent(f -> {
            throw new IllegalStateException("Friendship already exists with status: " + f.getStatus());
        });
        var friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus("PENDING");
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public Friendship acceptRequest(String userId, String requesterId) {
        var friendship = friendshipRepository.findBetween(requesterId, userId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        if (!friendship.getFriendId().equals(userId)) {
            throw new IllegalStateException("Only the recipient can accept");
        }
        friendship.setStatus("ACCEPTED");
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public void removeFriend(String userId, String friendId) {
        friendshipRepository.findBetween(userId, friendId)
                .ifPresent(friendshipRepository::delete);
    }

    public List<User> getFriends(String userId) {
        return friendshipRepository.findAcceptedFriends(userId).stream()
                .map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId())
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(u -> u != null)
                .toList();
    }

    public List<Friendship> getPendingRequests(String userId) {
        return friendshipRepository.findByFriendIdAndStatus(userId, "PENDING");
    }
}
