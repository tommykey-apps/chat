package com.example.chat.controller;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.dto.RoomRequest;
import com.example.chat.model.dto.RoomResponse;
import com.example.chat.service.ChatService;
import com.example.chat.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ChatService chatService;

    public RoomController(RoomService roomService, ChatService chatService) {
        this.roomService = roomService;
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@RequestBody RoomRequest request, Principal principal) {
        return roomService.createRoom(
                request.name(),
                request.description(),
                principal.getName(),
                principal.getName()
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

    @GetMapping("/{roomId}/messages")
    public Page<MessageResponse> getMessages(@PathVariable UUID roomId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(roomId, page, size);
    }
}
