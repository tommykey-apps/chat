package com.example.chat.service;

import com.example.chat.model.dto.RoomResponse;
import com.example.chat.model.entity.ChatRoom;
import com.example.chat.model.entity.RoomMember;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.RoomMemberRepository;
import com.example.chat.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(ChatRoomRepository chatRoomRepository,
                       ChatMessageRepository chatMessageRepository,
                       RoomMemberRepository roomMemberRepository,
                       UserRepository userRepository,
                       ChatService chatService,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public RoomResponse createRoom(String name, String description, String userId, String userName,
                                   List<String> memberIds) {
        var room = new ChatRoom();
        room.setName(name);
        room.setDescription(description);
        room.setCreatedBy(userId);
        chatRoomRepository.save(room);

        var owner = new RoomMember();
        owner.setRoomId(room.getId());
        owner.setUserId(userId);
        owner.setUserName(userName);
        owner.setRole("OWNER");
        roomMemberRepository.save(owner);

        int memberCount = 1;
        if (memberIds != null) {
            for (String memberId : memberIds) {
                if (memberId.equals(userId)) continue;
                var userOpt = userRepository.findById(memberId);
                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    var member = new RoomMember();
                    member.setRoomId(room.getId());
                    member.setUserId(user.getId());
                    member.setUserName(user.getDisplayName());
                    member.setRole("MEMBER");
                    roomMemberRepository.save(member);
                    memberCount++;
                }
            }
        }

        return toResponse(room, memberCount);
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
        var lastMsg = chatMessageRepository.findFirstByRoomIdOrderByCreatedAtDesc(room.getId());
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getCreatedBy(),
                room.getCreatedAt(),
                memberCount,
                lastMsg.map(ChatMessage::getContent).orElse(null),
                lastMsg.map(ChatMessage::getCreatedAt).orElse(null)
        );
    }
}
