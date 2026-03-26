package com.example.chat.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RoomMemberId implements Serializable {

    private UUID roomId;
    private String userId;

    public RoomMemberId() {
    }

    public RoomMemberId(UUID roomId, String userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomMemberId that)) return false;
        return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}
