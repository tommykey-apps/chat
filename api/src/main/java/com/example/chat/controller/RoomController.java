package com.example.chat.controller;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.dto.RoomRequest;
import com.example.chat.model.dto.RoomResponse;
import com.example.chat.service.ChatService;
import com.example.chat.service.RoomService;
import com.example.chat.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ChatService chatService;
    private final SearchService searchService;
    private final RedisTemplate<String, String> redisTemplate;

    public RoomController(RoomService roomService, ChatService chatService,
                          SearchService searchService, RedisTemplate<String, String> redisTemplate) {
        this.roomService = roomService;
        this.chatService = chatService;
        this.searchService = searchService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@RequestBody RoomRequest request, Principal principal) {
        return roomService.createRoom(
                request.name(),
                request.description(),
                principal.getName(),
                principal.getName(),
                request.memberIds()
        );
    }

    @GetMapping
    public List<RoomResponse> listRooms(Principal principal) {
        return roomService.listRooms(principal.getName());
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable UUID roomId) {
        return roomService.getRoom(roomId);
    }

    @PostMapping("/{roomId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void joinRoom(@PathVariable UUID roomId, Principal principal) {
        roomService.joinRoom(roomId, principal.getName(), principal.getName());
    }

    @DeleteMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRoom(@PathVariable UUID roomId, Principal principal) {
        roomService.leaveRoom(roomId, principal.getName());
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable UUID roomId, Principal principal) {
        roomService.deleteRoom(roomId, principal.getName());
    }

    @GetMapping("/{roomId}/messages")
    public Page<MessageResponse> getMessages(@PathVariable UUID roomId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(roomId, page, size);
    }

    @GetMapping("/{roomId}/messages/search")
    public Page<MessageResponse> searchMessages(@PathVariable UUID roomId,
                                                @RequestParam String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return searchService.searchMessages(roomId, q, page, size);
    }

    @GetMapping("/{roomId}/members")
    public List<Map<String, String>> getMembers(@PathVariable UUID roomId) {
        return roomService.getRoomMembersWithNames(roomId);
    }

    @GetMapping("/{roomId}/read-status")
    public Map<String, String> getReadStatus(@PathVariable UUID roomId) {
        var members = roomService.getRoomMembers(roomId);
        var result = new HashMap<String, String>();
        for (var userId : members) {
            var lastRead = redisTemplate.opsForValue().get("read:" + roomId + ":" + userId);
            if (lastRead != null) result.put(userId, lastRead);
        }
        return result;
    }
}
