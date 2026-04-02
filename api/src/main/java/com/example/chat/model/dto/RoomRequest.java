package com.example.chat.model.dto;

import java.util.List;

public record RoomRequest(String name, String description, List<String> memberIds) {
}
