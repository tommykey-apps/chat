package com.example.chat.repository;

import com.example.chat.model.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    List<Reaction> findByMessageId(UUID messageId);

    List<Reaction> findByMessageIdIn(List<UUID> messageIds);

    Optional<Reaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, String userId, String emoji);
}
