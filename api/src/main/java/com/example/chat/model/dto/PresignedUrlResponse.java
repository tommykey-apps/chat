package com.example.chat.model.dto;

public record PresignedUrlResponse(String uploadUrl, String s3Key) {
}
