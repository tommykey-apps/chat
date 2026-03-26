package com.example.chat.repository;

import com.example.chat.model.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {
}
