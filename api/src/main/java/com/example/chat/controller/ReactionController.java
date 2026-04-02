package com.example.chat.controller;

import com.example.chat.model.entity.Reaction;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ReactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages/{messageId}/reactions")
public class ReactionController {

    private final ReactionRepository reactionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ReactionController(ReactionRepository reactionRepository,
                              ChatMessageRepository chatMessageRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.reactionRepository = reactionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getReactions(@PathVariable UUID messageId) {
        return reactionRepository.findByMessageId(messageId).stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "emoji", e.getKey(),
                        "count", e.getValue().size(),
                        "userIds", e.getValue().stream().map(Reaction::getUserId).toList()
                ))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addReaction(@PathVariable UUID messageId,
                            @RequestBody Map<String, String> body,
                            Principal principal) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String emoji = body.get("emoji");
        var existing = reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, principal.getName(), emoji);
        if (existing.isPresent()) return;

        var reaction = new Reaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(principal.getName());
        reaction.setEmoji(emoji);
        reactionRepository.save(reaction);

        broadcastReactions(message.getRoomId(), messageId);
    }

    @DeleteMapping("/{emoji}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReaction(@PathVariable UUID messageId,
                               @PathVariable String emoji,
                               Principal principal) {
        var message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, principal.getName(), emoji)
                .ifPresent(reactionRepository::delete);
        broadcastReactions(message.getRoomId(), messageId);
    }

    private void broadcastReactions(UUID roomId, UUID messageId) {
        var reactions = reactionRepository.findByMessageId(messageId).stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji))
                .entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "emoji", e.getKey(),
                        "count", e.getValue().size(),
                        "userIds", e.getValue().stream().map(Reaction::getUserId).toList()
                ))
                .toList();
        messagingTemplate.convertAndSend("/topic/room." + roomId + ".reactions",
                Map.of("messageId", messageId.toString(), "reactions", reactions));
    }
}
