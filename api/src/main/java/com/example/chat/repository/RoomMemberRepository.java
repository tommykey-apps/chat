package com.example.chat.repository;

import com.example.chat.model.entity.RoomMember;
import com.example.chat.model.entity.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    List<RoomMember> findByRoomId(UUID roomId);

    List<RoomMember> findByUserId(String userId);

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, String userId);

    void deleteByRoomIdAndUserId(UUID roomId, String userId);
}
