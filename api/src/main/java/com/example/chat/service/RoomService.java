package com.example.chat.service;

import com.example.chat.model.dto.RoomResponse;
import com.example.chat.model.entity.ChatRoom;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.RoomMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(ChatRoomRepository chatRoomRepository,
                       RoomMemberRepository roomMemberRepository,
                       ChatService chatService,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public RoomResponse createRoom(String name, String description, String userId, String userName) {
        var room = new ChatRoom();
        room.setName(name);
        room.setDescription(description);
        room.setCreatedBy(userId);
        chatRoomRepository.save(room);

        var member = new RoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setUserName(userName);
        member.setRole("OWNER");
        roomMemberRepository.save(member);

        return toResponse(room, 1);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(String userId) {
        List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(membership -> {
                    var room = chatRoomRepository.findById(membership.getRoomId())
                            .orElseThrow(() -> new EntityNotFoundException("Room not found: " + membership.getRoomId()));
                    int memberCount = roomMemberRepository.findByRoomId(room.getId()).size();
                    return toResponse(room, memberCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(UUID roomId) {
        var room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        int memberCount = roomMemberRepository.findByRoomId(roomId).size();
        return toResponse(room, memberCount);
    }

    @Transactional
    public void joinRoom(UUID roomId, String userId, String userName) {
        if (roomMemberRepository.findByRoomIdAndUserId(roomId, userId).isPresent()) {
            return;
        }

        var member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setUserName(userName);
        roomMemberRepository.save(member);

        var systemMessage = chatService.saveMessage(roomId, "SYSTEM", "SYSTEM",
                userName + " joined", "SYSTEM");

        messagingTemplate.convertAndSend("/topic/room." + roomId,
                new com.example.chat.model.dto.MessageResponse(
                        systemMessage.getId(),
                        systemMessage.getSenderId(),
                        systemMessage.getSenderName(),
                        systemMessage.getContent(),
                        systemMessage.getMessageType(),
                        systemMessage.getCreatedAt()));
    }

    @Transactional
    public void leaveRoom(UUID roomId, String userId) {
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    private RoomResponse toResponse(ChatRoom room, int memberCount) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getCreatedBy(),
                room.getCreatedAt(),
                memberCount
        );
    }
}
