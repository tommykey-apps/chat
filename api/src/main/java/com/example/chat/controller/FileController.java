package com.example.chat.controller;

import com.example.chat.model.dto.PresignedUrlResponse;
import com.example.chat.service.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/presign-upload")
    public PresignedUrlResponse presignUpload(@RequestBody Map<String, String> request) {
        UUID roomId = UUID.fromString(request.get("roomId"));
        String fileName = request.get("fileName");
        String contentType = request.get("contentType");
        return fileService.generateUploadUrl(roomId, fileName, contentType);
    }

    @GetMapping("/presign-download/{s3Key}")
    public Map<String, String> presignDownload(@PathVariable String s3Key) {
        String downloadUrl = fileService.generateDownloadUrl(s3Key);
        return Map.of("downloadUrl", downloadUrl);
    }
}
