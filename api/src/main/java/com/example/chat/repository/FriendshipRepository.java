package com.example.chat.repository;

import com.example.chat.model.entity.Friendship;
import com.example.chat.model.entity.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId OR f.friendId = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriends(String userId);

    List<Friendship> findByFriendIdAndStatus(String friendId, String status);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :a AND f.friendId = :b) OR (f.userId = :b AND f.friendId = :a)")
    Optional<Friendship> findBetween(String a, String b);
}
