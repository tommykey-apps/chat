package com.example.chat.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;

@Document(indexName = "chat-messages")
@Setting(settingPath = "elasticsearch/settings.json")
public class ChatMessageDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String roomId;

    @Field(type = FieldType.Keyword)
    private String senderId;

    @Field(type = FieldType.Text)
    private String senderName;

    @Field(type = FieldType.Text, analyzer = "kuromoji_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String messageType;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    public ChatMessageDocument() {}

    public ChatMessageDocument(ChatMessage msg) {
        this.id = msg.getId().toString();
        this.roomId = msg.getRoomId().toString();
        this.senderId = msg.getSenderId();
        this.senderName = msg.getSenderName();
        this.content = msg.getContent();
        this.messageType = msg.getMessageType();
        this.createdAt = msg.getCreatedAt();
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public Instant getCreatedAt() { return createdAt; }
}
